package com.yourorg.nlsqlengine.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jeux de données NL <-> SQL attendus.
 * Valide que les requêtes SQL de référence passent bien la validation.
 */
class NlSqlExpectedQueriesTest {

    private SqlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SqlValidator();
    }

    static Stream<Arguments> expectedQueries() {
        return Stream.of(
                Arguments.of(
                        "Liste des personnages masculins",
                        "SELECT name, birth_year FROM people WHERE gender = 'male' ORDER BY name"
                ),
                Arguments.of(
                        "Combien de planètes dans la base ?",
                        "SELECT COUNT(id) AS total FROM planets"
                ),
                Arguments.of(
                        "Films réalisés par George Lucas",
                        "SELECT title, release_date FROM films WHERE director = 'George Lucas' ORDER BY release_date"
                ),
                Arguments.of(
                        "Personnages apparaissant dans A New Hope",
                        "SELECT p.name FROM people p JOIN film_characters fc ON p.id = fc.person_id JOIN films f ON f.id = fc.film_id WHERE f.title = 'A New Hope' ORDER BY p.name"
                ),
                Arguments.of(
                        "Vaisseaux pilotés par Luke Skywalker",
                        "SELECT s.name, s.model FROM starships s JOIN starship_pilots sp ON s.id = sp.starship_id JOIN people p ON p.id = sp.person_id WHERE p.name = 'Luke Skywalker'"
                ),
                Arguments.of(
                        "Top 5 planètes les plus peuplées",
                        "SELECT name, population FROM planets WHERE population != 'unknown' ORDER BY CAST(population AS BIGINT) DESC LIMIT 5"
                ),
                Arguments.of(
                        "Espèces de type mammal",
                        "SELECT name, language FROM species WHERE classification = 'mammal' ORDER BY name"
                ),
                Arguments.of(
                        "Personnages de Tatooine",
                        "SELECT p.name FROM people p JOIN planets pl ON p.homeworld_id = pl.id WHERE pl.name = 'Tatooine' ORDER BY p.name"
                ),
                Arguments.of(
                        "Nombre de personnages par film",
                        "SELECT f.title, COUNT(fc.person_id) AS nb_personnages FROM films f JOIN film_characters fc ON f.id = fc.film_id GROUP BY f.title ORDER BY nb_personnages DESC"
                ),
                Arguments.of(
                        "Vaisseaux les plus rapides",
                        "SELECT name, max_atmosphering_speed FROM starships WHERE max_atmosphering_speed != 'n/a' ORDER BY name LIMIT 10"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expectedQueries")
    void expectedQueryIsValid(String question, String expectedSql) {
        SqlValidationResult result = validator.validate(expectedSql);
        assertTrue(result.valid(),
                "La requête pour '" + question + "' devrait être valide : " + result.error());
    }
}
