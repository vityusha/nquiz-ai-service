package com.lainlab.db;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.transaction.annotation.Transactional;

import java.util.List;

@JdbcRepository(dialect = Dialect.H2)
@Transactional()
public interface QuestionRepository extends CrudRepository<QuestionEntity, Long> {

    Page<QuestionEntity> findByMode(String mode, Pageable pageable);
    Page<QuestionEntity> findByDifficulty(String difficulty, Pageable pageable);
    Page<QuestionEntity> findByType(String type, Pageable pageable);
    Page<QuestionEntity> findByLanguage(String language, Pageable pageable);
    Page<QuestionEntity> findByKeywords(String keywords, Pageable pageable);
    Page<QuestionEntity> findByTokenId(Long tokenId, Pageable pageable);

    @Query("INSERT INTO questions (token_id, question) VALUES (:tokenId, :question)")
    void insertQuestion(Long tokenId, String question);

    // Distinct filter values for the search page
    @Query("SELECT DISTINCT mode FROM questions WHERE mode IS NOT NULL AND mode != '' ORDER BY mode")
    List<String> findDistinctModes();

    @Query("SELECT DISTINCT difficulty FROM questions WHERE difficulty IS NOT NULL AND difficulty != '' ORDER BY difficulty")
    List<String> findDistinctDifficulties();

    @Query("SELECT DISTINCT type FROM questions WHERE type IS NOT NULL AND type != '' ORDER BY type")
    List<String> findDistinctTypes();

    @Query("SELECT DISTINCT language FROM questions WHERE language IS NOT NULL AND language != '' ORDER BY language")
    List<String> findDistinctLanguages();

    // Search with multiple optional filters
    @Query(
        value = "SELECT * FROM questions WHERE " +
                "(:mode = '' OR mode = :mode) AND " +
                "(:difficulty = '' OR difficulty = :difficulty) AND " +
                "(:type = '' OR type = :type) AND " +
                "(:language = '' OR language = :language) AND " +
                "(:keywords = '' OR keywords LIKE '%' || :keywords || '%') " +
                "ORDER BY created_at DESC",
        countQuery = "SELECT COUNT(*) FROM questions WHERE " +
                "(:mode = '' OR mode = :mode) AND " +
                "(:difficulty = '' OR difficulty = :difficulty) AND " +
                "(:type = '' OR type = :type) AND " +
                "(:language = '' OR language = :language) AND " +
                "(:keywords = '' OR keywords LIKE '%' || :keywords || '%')",
        nativeQuery = true
    )
    Page<QuestionEntity> search(String mode, String difficulty, String type, String language, String keywords, Pageable pageable);
}
