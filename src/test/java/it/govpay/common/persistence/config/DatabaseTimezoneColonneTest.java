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
package it.govpay.common.persistence.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import it.govpay.common.persistence.config.entity.DataSalvataEntity;
import jakarta.persistence.EntityManager;

/** Salvataggio e rilettura di un {@code OffsetDateTime} su una colonna {@code TIMESTAMP}. */
@SpringBootTest(classes = DatabaseTimezoneColonneTest.Applicazione.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:hsqldb:mem:fusodatabase",
        "spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver",
        "spring.datasource.username=SA",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.HSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "govpay.batch.job-repository.jdbc.enabled=false"
})
@DisplayName("Date sul database - colonne TIMESTAMP in ora Europe/Rome")
class DatabaseTimezoneColonneTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = DataSalvataEntity.class)
    static class Applicazione {
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest(name = "{0} salvato come {1}")
    @CsvSource({
            "2026-11-26T12:00:00+01:00, 2026-11-26T12:00:00",
            "2026-11-26T11:00:00Z,      2026-11-26T12:00:00",
            "2026-11-26T16:00:00+05:00, 2026-11-26T12:00:00",
            "2026-07-15T10:00:00Z,      2026-07-15T12:00:00",
            "2027-01-14T23:00:00Z,      2027-01-15T00:00:00"
    })
    @DisplayName("La colonna contiene l'ora italiana e la rilettura restituisce lo stesso istante")
    void salvaInOraItaliana(OffsetDateTime valore, LocalDateTime attesa) {
        Long id = transactionTemplate.execute(stato -> {
            DataSalvataEntity entity = new DataSalvataEntity();
            entity.setIstante(valore);
            entityManager.persist(entity);
            return entity.getId();
        });

        LocalDateTime colonna = jdbcTemplate.queryForObject(
                "SELECT istante FROM data_salvata WHERE id = ?", LocalDateTime.class, id);
        OffsetDateTime riletto = transactionTemplate.execute(
                stato -> entityManager.find(DataSalvataEntity.class, id).getIstante());

        assertEquals(attesa, colonna);
        assertEquals(valore.toInstant(), riletto.toInstant());
    }
}
