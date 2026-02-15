package com.yourorg.nlsqlengine.rag;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class SwapiImporter {

    private static final Logger LOG = Logger.getLogger(SwapiImporter.class);
    private static final String SWAPI_BASE = "https://swapi.dev/api";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Inject
    AgroalDataSource dataSource;

    public void importAll() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            createSchema(conn);

            Map<String, Integer> planetIds = importPlanets(conn);
            Map<String, Integer> speciesIds = importSpecies(conn, planetIds);
            Map<String, Integer> peopleIds = importPeople(conn, planetIds, speciesIds);
            Map<String, Integer> filmIds = importFilms(conn);
            Map<String, Integer> starshipIds = importStarships(conn);

            importFilmCharacters(conn, filmIds, peopleIds);
            importFilmPlanets(conn, filmIds, planetIds);
            importFilmStarships(conn, filmIds, starshipIds);
            importStarshipPilots(conn, starshipIds, peopleIds);

            conn.commit();
            LOG.info("Import Star Wars terminé avec succès");
        }
    }

    private void createSchema(Connection conn) throws Exception {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("schema/starwars-schema.sql")) {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = conn.createStatement()) {
                for (String s : sql.split(";")) {
                    String trimmed = s.strip();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        }
    }

    private Map<String, Integer> importPlanets(Connection conn) throws Exception {
        Map<String, Integer> ids = new HashMap<>();
        String sql = "INSERT INTO planets (name, rotation_period, orbital_period, diameter, climate, gravity, terrain, surface_water, population) VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (JsonNode node : fetchAll("/planets/")) {
                ps.setString(1, node.get("name").asText());
                ps.setString(2, node.get("rotation_period").asText());
                ps.setString(3, node.get("orbital_period").asText());
                ps.setString(4, node.get("diameter").asText());
                ps.setString(5, node.get("climate").asText());
                ps.setString(6, node.get("gravity").asText());
                ps.setString(7, node.get("terrain").asText());
                ps.setString(8, node.get("surface_water").asText());
                ps.setString(9, node.get("population").asText());
                ps.executeUpdate();
                var keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    ids.put(node.get("url").asText(), keys.getInt(1));
                }
            }
        }
        LOG.infof("Importé %d planètes", ids.size());
        return ids;
    }

    private Map<String, Integer> importSpecies(Connection conn, Map<String, Integer> planetIds) throws Exception {
        Map<String, Integer> ids = new HashMap<>();
        String sql = "INSERT INTO species (name, classification, designation, average_height, average_lifespan, language, homeworld_id) VALUES (?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (JsonNode node : fetchAll("/species/")) {
                ps.setString(1, node.get("name").asText());
                ps.setString(2, node.get("classification").asText());
                ps.setString(3, node.get("designation").asText());
                ps.setString(4, node.get("average_height").asText());
                ps.setString(5, node.get("average_lifespan").asText());
                ps.setString(6, node.get("language").asText());
                String hw = node.get("homeworld").asText();
                if (planetIds.containsKey(hw)) {
                    ps.setInt(7, planetIds.get(hw));
                } else {
                    ps.setNull(7, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
                var keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    ids.put(node.get("url").asText(), keys.getInt(1));
                }
            }
        }
        LOG.infof("Importé %d espèces", ids.size());
        return ids;
    }

    private Map<String, Integer> importPeople(Connection conn, Map<String, Integer> planetIds, Map<String, Integer> speciesIds) throws Exception {
        Map<String, Integer> ids = new HashMap<>();
        String sql = "INSERT INTO people (name, height, mass, hair_color, skin_color, eye_color, birth_year, gender, homeworld_id, species_id) VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (JsonNode node : fetchAll("/people/")) {
                ps.setString(1, node.get("name").asText());
                ps.setString(2, node.get("height").asText());
                ps.setString(3, node.get("mass").asText());
                ps.setString(4, node.get("hair_color").asText());
                ps.setString(5, node.get("skin_color").asText());
                ps.setString(6, node.get("eye_color").asText());
                ps.setString(7, node.get("birth_year").asText());
                ps.setString(8, node.get("gender").asText());
                String hw = node.get("homeworld").asText();
                if (planetIds.containsKey(hw)) {
                    ps.setInt(9, planetIds.get(hw));
                } else {
                    ps.setNull(9, java.sql.Types.INTEGER);
                }
                JsonNode speciesArr = node.get("species");
                if (speciesArr != null && speciesArr.size() > 0) {
                    String specUrl = speciesArr.get(0).asText();
                    if (speciesIds.containsKey(specUrl)) {
                        ps.setInt(10, speciesIds.get(specUrl));
                    } else {
                        ps.setNull(10, java.sql.Types.INTEGER);
                    }
                } else {
                    ps.setNull(10, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
                var keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    ids.put(node.get("url").asText(), keys.getInt(1));
                }
            }
        }
        LOG.infof("Importé %d personnages", ids.size());
        return ids;
    }

    private Map<String, Integer> importFilms(Connection conn) throws Exception {
        Map<String, Integer> ids = new HashMap<>();
        String sql = "INSERT INTO films (title, episode_id, director, producer, release_date) VALUES (?,?,?,?,?::date) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (JsonNode node : fetchAll("/films/")) {
                ps.setString(1, node.get("title").asText());
                ps.setInt(2, node.get("episode_id").asInt());
                ps.setString(3, node.get("director").asText());
                ps.setString(4, node.get("producer").asText());
                ps.setString(5, node.get("release_date").asText());
                ps.executeUpdate();
                var keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    ids.put(node.get("url").asText(), keys.getInt(1));
                }
            }
        }
        LOG.infof("Importé %d films", ids.size());
        return ids;
    }

    private Map<String, Integer> importStarships(Connection conn) throws Exception {
        Map<String, Integer> ids = new HashMap<>();
        String sql = "INSERT INTO starships (name, model, manufacturer, cost_in_credits, length, max_atmosphering_speed, crew, passengers, cargo_capacity, hyperdrive_rating, starship_class) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (JsonNode node : fetchAll("/starships/")) {
                ps.setString(1, node.get("name").asText());
                ps.setString(2, node.get("model").asText());
                ps.setString(3, node.get("manufacturer").asText());
                ps.setString(4, node.get("cost_in_credits").asText());
                ps.setString(5, node.get("length").asText());
                ps.setString(6, node.get("max_atmosphering_speed").asText());
                ps.setString(7, node.get("crew").asText());
                ps.setString(8, node.get("passengers").asText());
                ps.setString(9, node.get("cargo_capacity").asText());
                ps.setString(10, node.get("hyperdrive_rating").asText());
                ps.setString(11, node.get("starship_class").asText());
                ps.executeUpdate();
                var keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    ids.put(node.get("url").asText(), keys.getInt(1));
                }
            }
        }
        LOG.infof("Importé %d vaisseaux", ids.size());
        return ids;
    }

    private void importFilmCharacters(Connection conn, Map<String, Integer> filmIds, Map<String, Integer> peopleIds) throws Exception {
        String sql = "INSERT INTO film_characters (film_id, person_id) VALUES (?,?) ON CONFLICT DO NOTHING";
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonNode film : fetchAll("/films/")) {
                Integer filmId = filmIds.get(film.get("url").asText());
                if (filmId == null) continue;
                for (JsonNode charUrl : film.get("characters")) {
                    Integer personId = peopleIds.get(charUrl.asText());
                    if (personId == null) continue;
                    ps.setInt(1, filmId);
                    ps.setInt(2, personId);
                    ps.executeUpdate();
                    count++;
                }
            }
        }
        LOG.infof("Importé %d relations film-personnage", count);
    }

    private void importFilmPlanets(Connection conn, Map<String, Integer> filmIds, Map<String, Integer> planetIds) throws Exception {
        String sql = "INSERT INTO film_planets (film_id, planet_id) VALUES (?,?) ON CONFLICT DO NOTHING";
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonNode film : fetchAll("/films/")) {
                Integer filmId = filmIds.get(film.get("url").asText());
                if (filmId == null) continue;
                for (JsonNode planetUrl : film.get("planets")) {
                    Integer planetId = planetIds.get(planetUrl.asText());
                    if (planetId == null) continue;
                    ps.setInt(1, filmId);
                    ps.setInt(2, planetId);
                    ps.executeUpdate();
                    count++;
                }
            }
        }
        LOG.infof("Importé %d relations film-planète", count);
    }

    private void importFilmStarships(Connection conn, Map<String, Integer> filmIds, Map<String, Integer> starshipIds) throws Exception {
        String sql = "INSERT INTO film_starships (film_id, starship_id) VALUES (?,?) ON CONFLICT DO NOTHING";
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonNode film : fetchAll("/films/")) {
                Integer filmId = filmIds.get(film.get("url").asText());
                if (filmId == null) continue;
                for (JsonNode shipUrl : film.get("starships")) {
                    Integer shipId = starshipIds.get(shipUrl.asText());
                    if (shipId == null) continue;
                    ps.setInt(1, filmId);
                    ps.setInt(2, shipId);
                    ps.executeUpdate();
                    count++;
                }
            }
        }
        LOG.infof("Importé %d relations film-vaisseau", count);
    }

    private void importStarshipPilots(Connection conn, Map<String, Integer> starshipIds, Map<String, Integer> peopleIds) throws Exception {
        String sql = "INSERT INTO starship_pilots (starship_id, person_id) VALUES (?,?) ON CONFLICT DO NOTHING";
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonNode ship : fetchAll("/starships/")) {
                Integer shipId = starshipIds.get(ship.get("url").asText());
                if (shipId == null) continue;
                for (JsonNode pilotUrl : ship.get("pilots")) {
                    Integer personId = peopleIds.get(pilotUrl.asText());
                    if (personId == null) continue;
                    ps.setInt(1, shipId);
                    ps.setInt(2, personId);
                    ps.executeUpdate();
                    count++;
                }
            }
        }
        LOG.infof("Importé %d relations vaisseau-pilote", count);
    }

    private List<JsonNode> fetchAll(String path) throws Exception {
        List<JsonNode> results = new ArrayList<>();
        String url = SWAPI_BASE + path + "?format=json";
        while (url != null) {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = MAPPER.readTree(resp.body());
            for (JsonNode node : root.get("results")) {
                results.add(node);
            }
            JsonNode next = root.get("next");
            url = (next != null && !next.isNull()) ? next.asText() : null;
        }
        return results;
    }
}
