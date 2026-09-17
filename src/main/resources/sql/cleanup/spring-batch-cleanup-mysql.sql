-- =============================================================================
-- Svecchiamento dei metadati Spring Batch -- MySQL/MariaDB
--
-- Cancella tutte le righe relative alle esecuzioni anteriori a una data di
-- taglio, nell'ordine imposto dalle foreign key.
--
-- Riferimento temporale: COALESCE(END_TIME, START_TIME, CREATE_TIME).
-- CREATE_TIME e' NOT NULL nello schema di Spring Batch, quindi ogni esecuzione
-- ha sempre una data utile anche se non e' mai partita o non e' terminata.
--
-- Non c'e' filtro sullo stato: rientrano anche le esecuzioni non terminate
-- (STARTED, STARTING), che hanno END_TIME a NULL. E' una scelta esplicita,
-- serve a poter bonificare le esecuzioni rimaste appese.
--
-- ATTENZIONE: se un job e' in corso e la sua CREATE_TIME e' anteriore alla data
-- di taglio, le sue righe vengono cancellate e Spring Batch perde traccia
-- dell'esecuzione viva. Eseguire a batch fermi, oppure con una data di taglio
-- abbastanza indietro nel tempo.
--
-- Uso: il segnaposto della data di taglio va sostituito con un valore nel
-- formato 'YYYY-MM-DD HH:MM:SS'. Lo fa spring-batch-cleanup.sh, che sta in
-- questa stessa directory; a mano basta una sostituzione globale.
-- La data effettivamente in uso e' quella che compare qui sotto.
--
-- I cinque file di questa directory hanno corpo identico e differiscono solo
-- nel letterale di data: qui CAST('@@CUTOFF@@' AS DATETIME)
-- =============================================================================

-- 1/6 -- contesti degli step
DELETE FROM BATCH_STEP_EXECUTION_CONTEXT
WHERE STEP_EXECUTION_ID IN (
    SELECT se.STEP_EXECUTION_ID
    FROM BATCH_STEP_EXECUTION se
    JOIN BATCH_JOB_EXECUTION je ON se.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
    WHERE COALESCE(je.END_TIME, je.START_TIME, je.CREATE_TIME) < CAST('@@CUTOFF@@' AS DATETIME)
);

-- 2/6 -- step
DELETE FROM BATCH_STEP_EXECUTION
WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID
    FROM BATCH_JOB_EXECUTION
    WHERE COALESCE(END_TIME, START_TIME, CREATE_TIME) < CAST('@@CUTOFF@@' AS DATETIME)
);

-- 3/6 -- contesti delle esecuzioni
DELETE FROM BATCH_JOB_EXECUTION_CONTEXT
WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID
    FROM BATCH_JOB_EXECUTION
    WHERE COALESCE(END_TIME, START_TIME, CREATE_TIME) < CAST('@@CUTOFF@@' AS DATETIME)
);

-- 4/6 -- parametri delle esecuzioni
DELETE FROM BATCH_JOB_EXECUTION_PARAMS
WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID
    FROM BATCH_JOB_EXECUTION
    WHERE COALESCE(END_TIME, START_TIME, CREATE_TIME) < CAST('@@CUTOFF@@' AS DATETIME)
);

-- 5/6 -- esecuzioni
DELETE FROM BATCH_JOB_EXECUTION
WHERE COALESCE(END_TIME, START_TIME, CREATE_TIME) < CAST('@@CUTOFF@@' AS DATETIME);

-- 6/6 -- istanze rimaste senza alcuna esecuzione
DELETE FROM BATCH_JOB_INSTANCE
WHERE JOB_INSTANCE_ID NOT IN (
    SELECT JOB_INSTANCE_ID
    FROM BATCH_JOB_EXECUTION
);
