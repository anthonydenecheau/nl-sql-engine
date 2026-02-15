-- Schéma Star Wars pour NL-SQL-Engine

CREATE TABLE IF NOT EXISTS planets (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rotation_period VARCHAR(20),
    orbital_period VARCHAR(20),
    diameter VARCHAR(20),
    climate VARCHAR(100),
    gravity VARCHAR(50),
    terrain VARCHAR(200),
    surface_water VARCHAR(20),
    population VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS species (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    classification VARCHAR(50),
    designation VARCHAR(50),
    average_height VARCHAR(20),
    average_lifespan VARCHAR(20),
    language VARCHAR(50),
    homeworld_id INT REFERENCES planets(id)
);

CREATE TABLE IF NOT EXISTS people (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    height VARCHAR(20),
    mass VARCHAR(20),
    hair_color VARCHAR(50),
    skin_color VARCHAR(50),
    eye_color VARCHAR(50),
    birth_year VARCHAR(20),
    gender VARCHAR(20),
    homeworld_id INT REFERENCES planets(id),
    species_id INT REFERENCES species(id)
);

CREATE TABLE IF NOT EXISTS films (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    episode_id INT,
    director VARCHAR(100),
    producer VARCHAR(200),
    release_date DATE
);

CREATE TABLE IF NOT EXISTS starships (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    model VARCHAR(100),
    manufacturer VARCHAR(200),
    cost_in_credits VARCHAR(30),
    length VARCHAR(20),
    max_atmosphering_speed VARCHAR(20),
    crew VARCHAR(20),
    passengers VARCHAR(20),
    cargo_capacity VARCHAR(30),
    hyperdrive_rating VARCHAR(10),
    starship_class VARCHAR(50)
);

-- Tables de jointure

CREATE TABLE IF NOT EXISTS film_characters (
    film_id INT REFERENCES films(id),
    person_id INT REFERENCES people(id),
    PRIMARY KEY (film_id, person_id)
);

CREATE TABLE IF NOT EXISTS film_planets (
    film_id INT REFERENCES films(id),
    planet_id INT REFERENCES planets(id),
    PRIMARY KEY (film_id, planet_id)
);

CREATE TABLE IF NOT EXISTS film_starships (
    film_id INT REFERENCES films(id),
    starship_id INT REFERENCES starships(id),
    PRIMARY KEY (film_id, starship_id)
);

CREATE TABLE IF NOT EXISTS starship_pilots (
    starship_id INT REFERENCES starships(id),
    person_id INT REFERENCES people(id),
    PRIMARY KEY (starship_id, person_id)
);
