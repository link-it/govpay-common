/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2026 Link.it srl (http://www.link.it).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3, as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.govpay.common.logging.level;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import it.govpay.common.batch.dto.Problem;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller base per la gestione a runtime dei livelli di log (BP-LOG-1).
 *
 * <p>Le sottoclassi annotano la classe con {@code @RestController} e
 * {@code @RequestMapping} ed espongono i metodi con le proprie mappature, cosi'
 * da poterne scegliere path e protezione. Gli endpoint sono di amministrazione:
 * vanno protetti con controlli piu' restrittivi di quelli delle API operative
 * (BP-SEC-2) e, dove possibile, segregati a livello di rete.
 *
 * <p>Esempio:
 * <pre>
 * &#64;RestController
 * &#64;RequestMapping("/api/admin/logging")
 * &#64;PreAuthorize("hasRole('AMMINISTRATORE')")
 * public class LogLevelController extends AbstractLogLevelController {
 *
 *     public LogLevelController(DynamicLogLevelService service) {
 *         super(service);
 *     }
 *
 *     &#64;GetMapping("/loggers")
 *     public ResponseEntity&lt;List&lt;LivelloLoggerInfo&gt;&gt; loggers() {
 *         return getLoggers();
 *     }
 *
 *     &#64;PutMapping("/loggers/{logger}")
 *     public ResponseEntity&lt;Object&gt; set(&#64;PathVariable String logger,
 *             &#64;RequestBody LivelloLogRequest richiesta) {
 *         return setLivello(logger, richiesta);
 *     }
 *
 *     &#64;DeleteMapping("/loggers/{logger}")
 *     public ResponseEntity&lt;Object&gt; reset(&#64;PathVariable String logger) {
 *         return rimuoviLivello(logger);
 *     }
 *
 *     &#64;PostMapping("/loggers/refresh")
 *     public ResponseEntity&lt;Map&lt;String, String&gt;&gt; refresh() {
 *         return refreshLivelli();
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractLogLevelController {

    protected final DynamicLogLevelService logLevelService;

    protected AbstractLogLevelController(DynamicLogLevelService logLevelService) {
        this.logLevelService = logLevelService;
    }

    /**
     * Elenca i logger che ricadono nell'ambito gestibile dinamicamente, con
     * livello configurato, effettivo e iniziale.
     *
     * @return l'elenco dei logger gestibili
     */
    protected ResponseEntity<List<LivelloLoggerInfo>> getLoggers() {
        return ResponseEntity.ok(logLevelService.statoLogger());
    }

    /**
     * Restituisce i soli livelli attualmente imposti dalla configurazione dinamica.
     *
     * @return mappa logger-livello
     */
    protected ResponseEntity<Map<String, String>> getLivelliDinamici() {
        return ResponseEntity.ok(logLevelService.livelliCorrenti());
    }

    /**
     * Imposta il livello di un logger. La modifica ha effetto immediato su questo
     * nodo ed e' persistita su database: gli altri nodi la applicano al proprio
     * refresh.
     *
     * @param logger il nome del logger
     * @param richiesta il livello da impostare
     * @return {@code 204 No Content} in caso di successo, {@code 400} se logger o livello non sono validi
     */
    protected ResponseEntity<Object> setLivello(String logger, LivelloLogRequest richiesta) {
        String livello = richiesta != null ? richiesta.getLivello() : null;
        try {
            logLevelService.setLivello(logger, livello);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Richiesta di modifica del livello di log rifiutata per il logger '{}': {}",
                    logger, e.getMessage());
            return problemResponse(Problem.badRequest(e.getMessage()));
        }
    }

    /**
     * Riporta un logger al livello che aveva all'avvio, rimuovendolo dalla
     * configurazione dinamica.
     *
     * @param logger il nome del logger
     * @return {@code 204 No Content} in caso di successo, {@code 400} se il logger e' fuori ambito
     */
    protected ResponseEntity<Object> rimuoviLivello(String logger) {
        try {
            logLevelService.rimuoviLivello(logger);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Richiesta di ripristino del livello di log rifiutata per il logger '{}': {}",
                    logger, e.getMessage());
            return problemResponse(Problem.badRequest(e.getMessage()));
        }
    }

    /**
     * Forza la rilettura immediata della configurazione da database su questo nodo.
     *
     * @return i livelli applicati dopo la rilettura
     */
    protected ResponseEntity<Map<String, String>> refreshLivelli() {
        return ResponseEntity.ok(logLevelService.refresh());
    }

    /**
     * Riporta tutti i logger gestiti ai livelli iniziali e svuota la configurazione.
     *
     * @return {@code 204 No Content}
     */
    protected ResponseEntity<Object> resetLivelli() {
        logLevelService.reset();
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Object> problemResponse(Problem problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
