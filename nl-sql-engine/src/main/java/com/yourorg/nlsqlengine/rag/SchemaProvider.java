package com.yourorg.nlsqlengine.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@ApplicationScoped
public class SchemaProvider {

    private final String schemaDescription;

    public SchemaProvider() {
        this.schemaDescription = loadResource("schema/starwars-description.txt");
    }

    public String getSchemaDescription() {
        return schemaDescription;
    }

    public List<String> getBusinessRules() {
        return List.of(
                "Les valeurs 'unknown' ou 'n/a' signifient que l'information n'est pas disponible",
                "birth_year est au format BBY (Before Battle of Yavin) ou ABY (After Battle of Yavin)",
                "Pour joindre un personnage à ses films, utiliser la table film_characters",
                "Pour trouver les pilotes d'un vaisseau, utiliser la table starship_pilots",
                "Pour les planètes d'un film, utiliser la table film_planets",
                "Pour les vaisseaux d'un film, utiliser la table film_starships",
                "La relation entre species et planets est directe via species.homeworld_id (FK vers planets.id), il n'existe PAS de table species_planets",
                "La relation entre people et planets est directe via people.homeworld_id (FK vers planets.id), il n'existe PAS de table people_planets",
                "Les seules tables de jointure existantes sont : film_characters, film_planets, film_starships, starship_pilots. Ne jamais inventer d'autres tables de jointure",
                "IMPORTANT — Colonnes VARCHAR à valeur numérique : height, mass, population, diameter, rotation_period, orbital_period, average_height, average_lifespan, cost_in_credits, length, max_atmosphering_speed, crew, passengers, cargo_capacity, hyperdrive_rating, surface_water sont toutes de type VARCHAR (elles peuvent contenir 'unknown' ou 'n/a'). Règles obligatoires : (1) Toujours filtrer d'abord avec WHERE colonne ~ '^[0-9]+(\\.[0-9]+)?$' pour exclure les non-numériques. (2) Toujours utiliser CAST(colonne AS NUMERIC) pour CHAQUE colonne VARCHAR dans une comparaison, un tri, ou une opération arithmétique. Exemple correct : WHERE s.crew ~ '^[0-9]+(\\.[0-9]+)?$' AND CAST(s.crew AS NUMERIC) > 10. Exemple avec calcul : CAST(s.cargo_capacity AS NUMERIC) - CAST(s.crew AS NUMERIC). Ne JAMAIS utiliser une colonne VARCHAR directement avec un opérateur numérique (+, -, *, /, >, <, =nombre)"
        );
    }

    private String loadResource(String path) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Ressource introuvable : " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Erreur de lecture : " + path, e);
        }
    }
}
