package com.phraseforge.phraseforge_api.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Repository tests exercise the complete Flyway chain, including the demo seed.
 * Each test transaction removes that seed before creating its own fixture data;
 * Spring rolls the cleanup back after the test.
 */
public abstract class DatabaseFixtureCleanup {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearSeedData() {
        jdbcTemplate.update("DELETE FROM favorites");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM phrase_tags");
        jdbcTemplate.update("DELETE FROM phrase_categories");
        jdbcTemplate.update("DELETE FROM phrases");
        jdbcTemplate.update("DELETE FROM tags");
        jdbcTemplate.update("DELETE FROM categories");
        jdbcTemplate.update("DELETE FROM authors");
        jdbcTemplate.update("DELETE FROM users");
    }
}
