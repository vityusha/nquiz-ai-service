package com.lainlab.db;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.transaction.annotation.Transactional;

@JdbcRepository(dialect = Dialect.H2)
@Transactional
public interface ProcessedWebhookEventRepository extends CrudRepository<ProcessedWebhookEvent, String> {

    /**
     * @return 1 if the event was recorded, 0 if event_id already exists
     */
    @Query(
        value = "INSERT OR IGNORE INTO processed_webhook_events (event_id, token_id, amount) " +
                "VALUES (:eventId, :tokenId, :amount)",
        nativeQuery = true
    )
    int tryInsert(String eventId, long tokenId, int amount);
}
