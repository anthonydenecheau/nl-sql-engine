package com.yourorg.nlsqlengine.rag;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test d'intégration qui importe les données depuis swapi.dev.
 * Nécessite un accès réseau.
 * Lancer avec : mvn test -Dtest="SwapiImporterTest" -Dgroups="integration"
 */
@QuarkusTest
@Tag("integration")
class SwapiImporterTest {

    @Inject
    SwapiImporter importer;

    @Inject
    AgroalDataSource dataSource;

    @Test
    void importAllLoadsData() throws Exception {
        importer.importAll();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Vérifier que les tables contiennent des données
            assertRowCount(stmt, "planets", 60);
            assertRowCount(stmt, "people", 82);
            assertRowCount(stmt, "films", 6);
            assertRowCount(stmt, "starships", 36);
            assertRowCount(stmt, "species", 37);
            assertRowCount(stmt, "film_characters", 1);

            // Vérifier un personnage connu
            ResultSet rs = stmt.executeQuery("SELECT name FROM people WHERE name = 'Luke Skywalker'");
            assertTrue(rs.next(), "Luke Skywalker doit exister");
        }
    }

    private void assertRowCount(Statement stmt, String table, int minExpected) throws Exception {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table);
        rs.next();
        int count = rs.getInt(1);
        assertTrue(count >= minExpected,
                table + " devrait avoir au moins " + minExpected + " lignes, trouvé : " + count);
    }
}
