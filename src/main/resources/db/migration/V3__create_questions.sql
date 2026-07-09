CREATE TABLE questions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    token_id INTEGER NOT NULL,
    question TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    mode TEXT GENERATED ALWAYS AS (json_extract(question, '$.mode')) STORED,
    difficulty TEXT GENERATED ALWAYS AS (json_extract(question, '$.difficulty')) STORED,
    type TEXT GENERATED ALWAYS AS (json_extract(question, '$.type')) STORED,
    language TEXT GENERATED ALWAYS AS (json_extract(question, '$.language')) STORED,
    keywords TEXT GENERATED ALWAYS AS (json_extract(question, '$.keywords')) STORED,

    FOREIGN KEY (token_id) REFERENCES tokens(id)
);

CREATE INDEX idx_mode ON questions(mode);
CREATE INDEX idx_difficulty ON questions(difficulty);
CREATE INDEX idx_type ON questions(type);
CREATE INDEX idx_language ON questions(language);
CREATE INDEX idx_keywords ON questions(keywords);
