package com.lainlab.db;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.transaction.annotation.Transactional;

import java.util.Optional;

@JdbcRepository(dialect = Dialect.H2)
@Transactional(readOnly = false)
public interface TokenRepository extends CrudRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    Optional<Token> findByLicenseNo(int licenseNo);
}
