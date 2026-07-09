package com.lainlab.db;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.transaction.annotation.Transactional;

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
}
