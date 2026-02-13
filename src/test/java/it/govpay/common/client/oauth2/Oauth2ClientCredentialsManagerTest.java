package it.govpay.common.client.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.govpay.common.client.model.Connettore;
import it.govpay.common.client.oauth2.Oauth2ClientCredentialsManager.Oauth2TokenException;
import it.govpay.common.entity.TipoAutenticazione;

class Oauth2ClientCredentialsManagerTest {

    private Oauth2ClientCredentialsManager manager;

    private Connettore connettore;

    @BeforeEach
    void setUp() {
        manager = new Oauth2ClientCredentialsManager();
        connettore = Connettore.builder()
                .idConnettore("TEST_OAUTH2")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS)
                .oauth2ClientCredentialsClientId("test-client-id")
                .oauth2ClientCredentialsClientSecret("test-client-secret")
                .oauth2ClientCredentialsUrlTokenEndpoint("https://auth.test.com/token")
                .oauth2ClientCredentialsScope("read write")
                .build();
    }

    @AfterEach
    void tearDown() {
        manager.clearAll();
    }

    @Test
    @DisplayName("Token acquisition: ottiene access_token dal token endpoint")
    void testTokenAcquisition() {
        // Usiamo una sottoclasse che mocka refreshToken per evitare chiamate HTTP reali
        Oauth2ClientCredentialsManager spyManager = new Oauth2ClientCredentialsManager() {
            @Override
            Oauth2ClientCredentialsManager.CachedToken refreshToken(Connettore conn) {
                return new CachedToken("mock-access-token", System.currentTimeMillis() / 1000, 3600);
            }
        };

        String token = spyManager.getAccessToken("TEST_OAUTH2", connettore);
        assertNotNull(token);
        assertEquals("mock-access-token", token);
    }

    @Test
    @DisplayName("Token caching: due chiamate consecutive usano lo stesso token (una sola negoziazione)")
    void testTokenCaching() {
        AtomicInteger refreshCount = new AtomicInteger(0);
        Oauth2ClientCredentialsManager spyManager = new Oauth2ClientCredentialsManager() {
            @Override
            Oauth2ClientCredentialsManager.CachedToken refreshToken(Connettore conn) {
                refreshCount.incrementAndGet();
                return new CachedToken("cached-token", System.currentTimeMillis() / 1000, 3600);
            }
        };

        String token1 = spyManager.getAccessToken("TEST_OAUTH2", connettore);
        String token2 = spyManager.getAccessToken("TEST_OAUTH2", connettore);

        assertEquals("cached-token", token1);
        assertEquals("cached-token", token2);
        assertEquals(1, refreshCount.get(), "Il token endpoint dovrebbe essere chiamato una sola volta");
    }

    @Test
    @DisplayName("Token expiration: token scaduto viene rinegoziato")
    void testTokenExpiration() {
        AtomicInteger refreshCount = new AtomicInteger(0);
        Oauth2ClientCredentialsManager spyManager = new Oauth2ClientCredentialsManager() {
            @Override
            Oauth2ClientCredentialsManager.CachedToken refreshToken(Connettore conn) {
                int count = refreshCount.incrementAndGet();
                if (count == 1) {
                    // Primo token: scade immediatamente (issuedAt nel passato)
                    return new CachedToken("expired-token", 0, 0);
                }
                return new CachedToken("new-token", System.currentTimeMillis() / 1000, 3600);
            }
        };

        String token1 = spyManager.getAccessToken("TEST_OAUTH2", connettore);
        assertEquals("expired-token", token1);

        String token2 = spyManager.getAccessToken("TEST_OAUTH2", connettore);
        assertEquals("new-token", token2);
        assertEquals(2, refreshCount.get(), "Il token dovrebbe essere rinegoziato dopo scadenza");
    }

    @Test
    @DisplayName("invalidateToken: rimuove il token dalla cache e forza rinegoziazione")
    void testInvalidateToken() {
        AtomicInteger refreshCount = new AtomicInteger(0);
        Oauth2ClientCredentialsManager spyManager = new Oauth2ClientCredentialsManager() {
            @Override
            Oauth2ClientCredentialsManager.CachedToken refreshToken(Connettore conn) {
                int count = refreshCount.incrementAndGet();
                return new CachedToken("token-" + count, System.currentTimeMillis() / 1000, 3600);
            }
        };

        String token1 = spyManager.getAccessToken("TEST_OAUTH2", connettore);
        assertEquals("token-1", token1);

        spyManager.invalidateToken("TEST_OAUTH2");

        String token2 = spyManager.getAccessToken("TEST_OAUTH2", connettore);
        assertEquals("token-2", token2);
        assertEquals(2, refreshCount.get());
    }

    @Test
    @DisplayName("clearAll: svuota tutta la cache")
    void testClearAll() {
        AtomicInteger refreshCount = new AtomicInteger(0);
        Oauth2ClientCredentialsManager spyManager = new Oauth2ClientCredentialsManager() {
            @Override
            Oauth2ClientCredentialsManager.CachedToken refreshToken(Connettore conn) {
                refreshCount.incrementAndGet();
                return new CachedToken("token", System.currentTimeMillis() / 1000, 3600);
            }
        };

        spyManager.getAccessToken("KEY_A", connettore);
        spyManager.getAccessToken("KEY_B", connettore);
        assertEquals(2, refreshCount.get());

        spyManager.clearAll();

        spyManager.getAccessToken("KEY_A", connettore);
        spyManager.getAccessToken("KEY_B", connettore);
        assertEquals(4, refreshCount.get(), "Dopo clearAll tutti i token devono essere rinegoziati");
    }

    @Test
    @DisplayName("Errore token endpoint: lancia Oauth2TokenException")
    void testTokenEndpointError() {
        // Il manager reale tenterà una connessione HTTP a un endpoint inesistente
        assertThrows(Oauth2TokenException.class,
                () -> manager.getAccessToken("TEST_OAUTH2", connettore));
    }

    @Test
    @DisplayName("Errore token endpoint: messaggio contiene id connettore")
    void testTokenEndpointErrorMessage() {
        Oauth2TokenException ex = assertThrows(Oauth2TokenException.class,
                () -> manager.getAccessToken("TEST_OAUTH2", connettore));
        assertTrue(ex.getMessage().contains("TEST_OAUTH2"),
                "Il messaggio di errore deve contenere l'id del connettore");
    }

    @Test
    @DisplayName("Thread safety: un solo refresh per chiave sotto accesso concorrente")
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        AtomicInteger refreshCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        CountDownLatch refreshEnteredLatch = new CountDownLatch(1);
        AtomicReference<String> lastToken = new AtomicReference<>();

        Oauth2ClientCredentialsManager spyManager = new Oauth2ClientCredentialsManager() {
            @Override
            Oauth2ClientCredentialsManager.CachedToken refreshToken(Connettore conn) {
                refreshCount.incrementAndGet();
                // Segnala che il refresh è in corso e attende che tutti i thread siano partiti
                refreshEnteredLatch.countDown();
                return new CachedToken("concurrent-token", System.currentTimeMillis() / 1000, 3600);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String token = spyManager.getAccessToken("CONCURRENT_KEY", connettore);
                    lastToken.set(token);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Avvia tutti i thread simultaneamente
        doneLatch.await();
        executor.shutdown();

        assertEquals("concurrent-token", lastToken.get());
        assertEquals(1, refreshCount.get(),
                "Con accesso concorrente, il refresh deve avvenire una sola volta per chiave");
    }

    @Test
    @DisplayName("CachedToken: token non scaduto")
    void testCachedTokenNotExpired() {
        long now = System.currentTimeMillis() / 1000;
        var token = new Oauth2ClientCredentialsManager.CachedToken("test", now, 3600);
        assertTrue(!token.isExpired(), "Token appena creato con 3600s non deve essere scaduto");
    }

    @Test
    @DisplayName("CachedToken: token scaduto")
    void testCachedTokenExpired() {
        // issuedAt nel passato, expiresIn = 0
        var token = new Oauth2ClientCredentialsManager.CachedToken("test", 0, 0);
        assertTrue(token.isExpired(), "Token con expires_in=0 e issuedAt=0 deve essere scaduto");
    }

    @Test
    @DisplayName("CachedToken: token quasi scaduto (entro safety margin)")
    void testCachedTokenWithinSafetyMargin() {
        long now = System.currentTimeMillis() / 1000;
        // expiresIn = 29s, meno del safety margin di 30s
        var token = new Oauth2ClientCredentialsManager.CachedToken("test", now, 29);
        assertTrue(token.isExpired(), "Token entro il safety margin deve risultare scaduto");
    }
}
