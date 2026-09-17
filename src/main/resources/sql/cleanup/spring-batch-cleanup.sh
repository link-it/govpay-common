#!/bin/bash
# =============================================================================
# Svecchiamento dei metadati Spring Batch.
#
# Cancella tutte le righe relative alle esecuzioni anteriori a una data di
# taglio, su qualunque dei database supportati. Il SQL vero e proprio sta nei
# file spring-batch-cleanup-<vendor>.sql di questa stessa directory: questo
# script si limita a scegliere il file giusto, sostituire la data e passarlo a
# SqlTool con la stessa connessione usata dall'init del container.
#
# Nelle immagini Docker dei batch GovPay si trova in /opt/sql/cleanup/.
# =============================================================================
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

log_info()  { echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $*"; }
log_error() { echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $*" >&2; }

printHelp() {
cat <<EOH
Uso: $(basename "$0") -d <data> [-t <tipo-db>] [-n] [-h]

  -d <data>     Data di taglio: vengono cancellate tutte le esecuzioni
                anteriori. Formati accettati:
                  YYYY-MM-DD              (interpretata come 00:00:00)
                  "YYYY-MM-DD HH:MM:SS"
  -t <tipo-db>  postgresql | mysql | mariadb | oracle | sqlserver | hsqldb.
                Default: \$GOVPAY_DB_TYPE.
  -n            Stampa il SQL che verrebbe eseguito e termina, senza toccare
                il database.
  -h            Questo aiuto.

Variabili d'ambiente di connessione (le stesse dell'init del container):
  GOVPAY_DB_TYPE, GOVPAY_DB_SERVER (host[:porta]), GOVPAY_DB_NAME,
  GOVPAY_DB_USER, GOVPAY_DB_PASSWORD, GOVPAY_DS_DRIVER_CLASS,
  GOVPAY_DS_JDBC_LIBS, HSQLDB_FULLVERSION, GOVPAY_SQLTOOL_JAR.

ATTENZIONE: non c'e' filtro sullo stato delle esecuzioni. Un job in corso la
cui CREATE_TIME e' anteriore alla data di taglio viene cancellato e Spring
Batch ne perde traccia. Eseguire a batch fermi.
EOH
}

CUTOFF=
DB_TYPE="${GOVPAY_DB_TYPE:-}"
DRY_RUN=false

while getopts "d:t:nh" opt; do
    case "${opt}" in
        d) CUTOFF="${OPTARG}" ;;
        t) DB_TYPE="${OPTARG}" ;;
        n) DRY_RUN=true ;;
        h) printHelp; exit 0 ;;
        *) printHelp; exit 1 ;;
    esac
done

if [ -z "${CUTOFF}" ]; then
    log_error "Data di taglio non indicata."
    printHelp
    exit 1
fi

# Normalizzazione e validazione della data
if [[ "${CUTOFF}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    CUTOFF="${CUTOFF} 00:00:00"
elif ! [[ "${CUTOFF}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}\ [0-9]{2}:[0-9]{2}:[0-9]{2}$ ]]; then
    log_error "Formato data non valido: '${CUTOFF}'. Attesi YYYY-MM-DD oppure 'YYYY-MM-DD HH:MM:SS'."
    exit 1
fi
# Rifiuta date che il calendario non ammette (es. 2026-02-30)
if ! date -d "${CUTOFF}" '+%Y-%m-%d %H:%M:%S' >/dev/null 2>&1; then
    log_error "Data inesistente: '${CUTOFF}'."
    exit 1
fi

if [ -z "${DB_TYPE}" ]; then
    log_error "Tipo database non indicato: usare -t oppure valorizzare GOVPAY_DB_TYPE."
    exit 1
fi

# MariaDB usa il dialetto MySQL
SQL_VENDOR="${DB_TYPE}"
[ "${DB_TYPE}" == "mariadb" ] && SQL_VENDOR="mysql"

SQL_TEMPLATE="${SCRIPT_DIR}/spring-batch-cleanup-${SQL_VENDOR}.sql"
if [ ! -f "${SQL_TEMPLATE}" ]; then
    log_error "Tipo database non supportato: ${DB_TYPE} (atteso ${SQL_TEMPLATE})"
    log_error "File disponibili in ${SCRIPT_DIR}:"
    ls -la "${SCRIPT_DIR}" 2>/dev/null
    exit 1
fi

SQL_RESO="$(mktemp -t spring-batch-cleanup-XXXXXX.sql)"
trap 'rm -f "${SQL_RESO}"' EXIT
sed "s/@@CUTOFF@@/${CUTOFF}/g" "${SQL_TEMPLATE}" > "${SQL_RESO}"

if grep -q '@@CUTOFF@@' "${SQL_RESO}"; then
    log_error "Sostituzione della data non riuscita su ${SQL_TEMPLATE}."
    exit 1
fi

if ${DRY_RUN}; then
    log_info "Dry run: SQL per ${SQL_VENDOR} con data di taglio ${CUTOFF}"
    echo
    cat "${SQL_RESO}"
    exit 0
fi

##############################################################################
# CONNESSIONE
##############################################################################

for VAR in GOVPAY_DB_SERVER GOVPAY_DB_NAME GOVPAY_DB_USER GOVPAY_DB_PASSWORD GOVPAY_DS_DRIVER_CLASS; do
    if [ -z "${!VAR:-}" ]; then
        log_error "Variabile d'ambiente ${VAR} non valorizzata."
        exit 1
    fi
done

IFS=':' read -r DB_HOST DB_PORT <<< "${GOVPAY_DB_SERVER}"
if [ -z "${DB_PORT}" ] || [ "${DB_PORT}" == "${DB_HOST}" ]; then
    case "${DB_TYPE}" in
        postgresql) DB_PORT=5432 ;;
        mysql|mariadb) DB_PORT=3306 ;;
        oracle) DB_PORT=1521 ;;
        sqlserver) DB_PORT=1433 ;;
        *) DB_PORT=5432 ;;
    esac
fi

case "${DB_TYPE}" in
    postgresql)
        JDBC_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${GOVPAY_DB_NAME}"
        START_TRANSACTION="START TRANSACTION;" ;;
    mysql|mariadb)
        JDBC_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${GOVPAY_DB_NAME}"
        START_TRANSACTION="START TRANSACTION;" ;;
    oracle)
        if [ "${GOVPAY_ORACLE_JDBC_URL_TYPE:-servicename}" == "servicename" ]; then
            JDBC_URL="jdbc:oracle:thin:@//${DB_HOST}:${DB_PORT}/${GOVPAY_DB_NAME}"
        else
            JDBC_URL="jdbc:oracle:thin:@${DB_HOST}:${DB_PORT}:${GOVPAY_DB_NAME}"
        fi
        START_TRANSACTION="" ;;
    sqlserver)
        JDBC_URL="jdbc:sqlserver://${DB_HOST}:${DB_PORT};databaseName=${GOVPAY_DB_NAME}"
        START_TRANSACTION="BEGIN TRANSACTION;" ;;
    hsqldb)
        JDBC_URL="jdbc:hsqldb:hsql://${DB_HOST}:${DB_PORT}/${GOVPAY_DB_NAME}"
        # HSQLDB non accetta START TRANSACTION nudo: vuole ISOLATION LEVEL
        # oppure READ WRITE. Il livello e' lo stesso dichiarato nel file rc.
        START_TRANSACTION="START TRANSACTION ISOLATION LEVEL READ COMMITTED;" ;;
    *)
        log_error "Tipo database non supportato per la connessione: ${DB_TYPE}"
        exit 1 ;;
esac

if [ -n "${GOVPAY_DS_CONN_PARAM:-}" ]; then
    if [[ "${JDBC_URL}" == *"?"* ]]; then
        JDBC_URL="${JDBC_URL}&${GOVPAY_DS_CONN_PARAM}"
    else
        JDBC_URL="${JDBC_URL}?${GOVPAY_DS_CONN_PARAM}"
    fi
fi

SQLTOOL_RC_FILE="$(mktemp -t sqltool-cleanup-XXXXXX.rc)"
trap 'rm -f "${SQL_RESO}" "${SQLTOOL_RC_FILE}"' EXIT
cat > "${SQLTOOL_RC_FILE}" <<EORC
urlid cleanup_db
url ${JDBC_URL}
username ${GOVPAY_DB_USER}
password ${GOVPAY_DB_PASSWORD}
driver ${GOVPAY_DS_DRIVER_CLASS}
transiso TRANSACTION_READ_COMMITTED
charset UTF-8
EORC

log_info "========================================"
log_info "Svecchiamento metadati Spring Batch"
log_info "========================================"
log_info "Tipo database:  ${DB_TYPE} (dialetto ${SQL_VENDOR})"
log_info "Server:         ${DB_HOST}:${DB_PORT}"
log_info "Database:       ${GOVPAY_DB_NAME}"
log_info "Data di taglio:  ${CUTOFF}"
log_info "Vengono cancellate tutte le esecuzioni anteriori, in qualunque stato."

# Il percorso di sqltool.jar e' quello dell'immagine; GOVPAY_SQLTOOL_JAR serve a
# eseguire lo script fuori dal container, dove /opt non c'e'.
SQLTOOL_JAR="${GOVPAY_SQLTOOL_JAR:-/opt/hsqldb-${HSQLDB_FULLVERSION:-2.7.4}/hsqldb/lib/sqltool.jar}"
INVOCAZIONE_CLIENT="-Dfile.encoding=UTF-8 -cp ${GOVPAY_DS_JDBC_LIBS:-/opt/jdbc-drivers}/*:${SQLTOOL_JAR} org.hsqldb.cmdline.SqlTool --rcFile=${SQLTOOL_RC_FILE}"

# Conteggio righe per tabella: una sola istruzione, portabile su tutti i dialetti
CONTEGGI_SQL="SELECT 'BATCH_JOB_INSTANCE' AS TABELLA, COUNT(*) AS RIGHE FROM BATCH_JOB_INSTANCE \
UNION ALL SELECT 'BATCH_JOB_EXECUTION', COUNT(*) FROM BATCH_JOB_EXECUTION \
UNION ALL SELECT 'BATCH_JOB_EXECUTION_PARAMS', COUNT(*) FROM BATCH_JOB_EXECUTION_PARAMS \
UNION ALL SELECT 'BATCH_JOB_EXECUTION_CONTEXT', COUNT(*) FROM BATCH_JOB_EXECUTION_CONTEXT \
UNION ALL SELECT 'BATCH_STEP_EXECUTION', COUNT(*) FROM BATCH_STEP_EXECUTION \
UNION ALL SELECT 'BATCH_STEP_EXECUTION_CONTEXT', COUNT(*) FROM BATCH_STEP_EXECUTION_CONTEXT;"

conteggi() {
    java ${INVOCAZIONE_CLIENT} --sql="${CONTEGGI_SQL}" cleanup_db 2>/dev/null \
      | grep -E '^BATCH_' | tr -s ' \t' ' '
}

PRIMA="$(conteggi)"
if [ -z "${PRIMA}" ]; then
    log_error "Impossibile leggere le tabelle dei metadati Spring Batch."
    log_error "Verificare connessione e che lo schema batch sia presente su ${GOVPAY_DB_NAME}."
    exit 1
fi

# Lo script finale include la transazione, cosi' SqlTool lo esegue come file e
# non come sessione interattiva: niente banner e uscita non nulla sugli errori.
SQL_ESEC="$(mktemp -t spring-batch-cleanup-esec-XXXXXX.sql)"
trap 'rm -f "${SQL_RESO}" "${SQLTOOL_RC_FILE}" "${SQL_ESEC}"' EXIT
{
    [ -n "${START_TRANSACTION}" ] && echo "${START_TRANSACTION}"
    cat "${SQL_RESO}"
    echo "COMMIT;"
} > "${SQL_ESEC}"

if ! java ${INVOCAZIONE_CLIENT} cleanup_db "${SQL_ESEC}"; then
    log_error "Svecchiamento fallito: transazione non committata, nessuna riga cancellata."
    exit 1
fi

DOPO="$(conteggi)"

log_info "Righe per tabella, prima e dopo:"
while read -r TAB N_PRIMA; do
    N_DOPO="$(echo "${DOPO}" | awk -v t="${TAB}" '$1==t {print $2}')"
    printf '[INFO] %-30s %8s -> %-8s (-%s)\n' "${TAB}" "${N_PRIMA}" "${N_DOPO}" "$((N_PRIMA - N_DOPO))"
done <<< "${PRIMA}"

log_info "========================================"
log_info "Svecchiamento completato"
log_info "========================================"
