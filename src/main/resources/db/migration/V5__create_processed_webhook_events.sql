CREATE TABLE processed_webhook_events (
    event_id TEXT PRIMARY KEY,
    token_id INTEGER,
    amount INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
