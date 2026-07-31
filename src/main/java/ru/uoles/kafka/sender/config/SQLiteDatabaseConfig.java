package ru.uoles.kafka.sender.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** Настраивает SQLite перед запуском Liquibase и предоставляет JDBC-шаблон. */
@Configuration
public class SQLiteDatabaseConfig {

    /** Создаёт JDBC-шаблон; схему создаёт Liquibase. */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
