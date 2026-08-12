package com.lainlab.db;

import com.lainlab.dto.LicenseStats;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.H2)
@Transactional()
public interface TokenRepository extends CrudRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    Optional<Token> findByLicenseNo(int licenseNo);

    @Query("UPDATE tokens SET balance = balance - :count, total = total + :count WHERE id = :tokenId AND balance >= :count")
    int chargeBalance(long tokenId, int count);

    @Query("UPDATE tokens SET balance = balance + :count, total = total - :count WHERE id = :tokenId AND total >= :count")
    int refundBalance(long tokenId, int count);

    @Query("UPDATE tokens SET balance = balance + :amount WHERE id = :tokenId")
    int addBalance(long tokenId, int amount);

    @Query("SELECT COUNT(*) FROM tokens WHERE admin != 0")
    long countAdmins();

    @Query(
        value = "SELECT COUNT(DISTINCT t.license_no) FROM ai_response_log arl " +
                "INNER JOIN tokens t ON arl.token_id = t.id",
        nativeQuery = true
    )
    long countDistinctActiveLicenseNo();

    @Query(
        value = "SELECT COUNT(*) FROM ai_response_log",
        nativeQuery = true
    )
    long countAiRequests();

    @Query(
        value = "SELECT t.license_no AS license_no, t.license_org AS license_org, t.email AS email, " +
                "t.balance AS balance, " +
                "t.total AS total, " +
                "COUNT(DISTINCT arl.id) AS ai_requests, COUNT(DISTINCT q.id) AS questions_stored " +
                "FROM tokens t " +
                "LEFT JOIN ai_response_log arl ON arl.token_id = t.id " +
                "LEFT JOIN questions q ON q.token_id = t.id " +
                "WHERE t.admin = 0 " +
                "GROUP BY t.license_no, t.license_org, t.email, t.balance, t.total " +
                "ORDER BY ai_requests DESC",
        nativeQuery = true
    )
    List<LicenseStats> findLicenseStats();
}
