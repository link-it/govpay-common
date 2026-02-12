package it.govpay.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnettoreEntityTest {

    @Test
    void testNoArgsConstructor() {
        ConnettoreEntity entity = new ConnettoreEntity();

        assertNull(entity.getId());
        assertNull(entity.getCodConnettore());
        assertNull(entity.getCodProprieta());
        assertNull(entity.getValore());
    }

    @Test
    void testAllArgsConstructor() {
        ConnettoreEntity entity = new ConnettoreEntity(1L, "TEST_CONN", "URL", "https://test.com");

        assertEquals(1L, entity.getId());
        assertEquals("TEST_CONN", entity.getCodConnettore());
        assertEquals("URL", entity.getCodProprieta());
        assertEquals("https://test.com", entity.getValore());
    }

    @Test
    void testBuilder() {
        ConnettoreEntity entity = ConnettoreEntity.builder()
                .id(2L)
                .codConnettore("BUILDER_CONN")
                .codProprieta("HTTPUSER")
                .valore("admin")
                .build();

        assertEquals(2L, entity.getId());
        assertEquals("BUILDER_CONN", entity.getCodConnettore());
        assertEquals("HTTPUSER", entity.getCodProprieta());
        assertEquals("admin", entity.getValore());
    }

    @Test
    void testSettersAndGetters() {
        ConnettoreEntity entity = new ConnettoreEntity();

        entity.setId(3L);
        entity.setCodConnettore("SETTER_CONN");
        entity.setCodProprieta("HTTPPASSW");
        entity.setValore("secret123");

        assertEquals(3L, entity.getId());
        assertEquals("SETTER_CONN", entity.getCodConnettore());
        assertEquals("HTTPPASSW", entity.getCodProprieta());
        assertEquals("secret123", entity.getValore());
    }

    @Test
    void testEqualsAndHashCode() {
        ConnettoreEntity entity1 = ConnettoreEntity.builder()
                .id(1L)
                .codConnettore("CONN")
                .codProprieta("PROP")
                .valore("VAL")
                .build();

        ConnettoreEntity entity2 = ConnettoreEntity.builder()
                .id(1L)
                .codConnettore("CONN")
                .codProprieta("PROP")
                .valore("VAL")
                .build();

        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testNotEquals() {
        ConnettoreEntity entity1 = ConnettoreEntity.builder()
                .id(1L)
                .codConnettore("CONN1")
                .codProprieta("PROP")
                .valore("VAL")
                .build();

        ConnettoreEntity entity2 = ConnettoreEntity.builder()
                .id(2L)
                .codConnettore("CONN2")
                .codProprieta("PROP")
                .valore("VAL")
                .build();

        assertNotEquals(entity1, entity2);
    }

    @Test
    void testToString() {
        ConnettoreEntity entity = ConnettoreEntity.builder()
                .id(1L)
                .codConnettore("TEST")
                .codProprieta("URL")
                .valore("https://test.com")
                .build();

        String toString = entity.toString();
        assertTrue(toString.contains("TEST"));
        assertTrue(toString.contains("URL"));
        assertTrue(toString.contains("https://test.com"));
    }

    @Test
    void testBuilderDefaults() {
        ConnettoreEntity entity = ConnettoreEntity.builder().build();

        assertNull(entity.getId());
        assertNull(entity.getCodConnettore());
        assertNull(entity.getCodProprieta());
        assertNull(entity.getValore());
    }

    @Test
    void testDifferentProperties() {
        ConnettoreEntity urlEntity = ConnettoreEntity.builder()
                .codConnettore("API_CONN")
                .codProprieta("URL")
                .valore("https://api.example.com")
                .build();

        ConnettoreEntity authEntity = ConnettoreEntity.builder()
                .codConnettore("API_CONN")
                .codProprieta("TIPOAUTENTICAZIONE")
                .valore("HTTPBasic")
                .build();

        assertEquals("API_CONN", urlEntity.getCodConnettore());
        assertEquals("API_CONN", authEntity.getCodConnettore());
        assertNotEquals(urlEntity.getCodProprieta(), authEntity.getCodProprieta());
        assertNotEquals(urlEntity.getValore(), authEntity.getValore());
    }
}
