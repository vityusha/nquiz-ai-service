package com.lainlab.db;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.transaction.annotation.Transactional;

import java.util.List;

@JdbcRepository(dialect = Dialect.H2)
@Transactional()
public interface AiResponseLogRepository extends CrudRepository<AiResponseLog, Long> {

    List<AiResponseLog> findByTokenIdOrderByCreatedAtDesc(Long tokenId);
    List<AiResponseLog> findAllOrderByCreatedAtDesc(Pageable pageable);
    List<AiResponseLog> findByIpAddress(String ipAddress);
}
