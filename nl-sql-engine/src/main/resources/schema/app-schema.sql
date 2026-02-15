CREATE TABLE IF NOT EXISTS domains (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS domain_tables (
    domain_id INTEGER REFERENCES domains(id),
    table_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (domain_id, table_name)
);

CREATE TABLE IF NOT EXISTS saved_prompts (
    id SERIAL PRIMARY KEY,
    domain_id INTEGER REFERENCES domains(id),
    question TEXT NOT NULL,
    sql_generated TEXT,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
