package ru.uoles.kafka.sender.security.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.uoles.kafka.sender.security.model.UserAccount;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC-репозиторий учётных записей и ролей пользователей. */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String FIND_BY_USERNAME = """
            SELECT u.id, u.username, u.password_hash, u.display_name, u.enabled, u.created_at, u.updated_at,
                   r.name AS role_name
            FROM users u
            LEFT JOIN user_roles ur ON ur.user_id = u.id
            LEFT JOIN roles r ON r.id = ur.role_id
            WHERE u.username = ?
            ORDER BY r.name
            """;

    private static final String INSERT_USER = """
            INSERT INTO users(id, username, password_hash, display_name, enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FIND_ROLE_ID = "SELECT id FROM roles WHERE name = ?";
    private static final String INSERT_USER_ROLE = "INSERT INTO user_roles(user_id, role_id) VALUES (?, ?)";

    /** Загружает пользователя вместе с ролями по нормализованному имени. */
    public Optional<UserAccount> findByUsername(String username) {
        List<UserAccount> accounts = jdbcTemplate.query(FIND_BY_USERNAME, this::mapAccount, username);
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        UserAccount first = accounts.get(0);
        List<String> roles = accounts.stream().flatMap(account -> account.roles().stream()).distinct().toList();
        return Optional.of(new UserAccount(first.id(), first.username(), first.passwordHash(), first.displayName(),
                first.enabled(), first.createdAt(), first.updatedAt(), roles));
    }

    /** Создаёт пользователя и назначает ему роль. */
    public UserAccount create(String username, String passwordHash, String displayName, String role) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        try {
            jdbcTemplate.update(INSERT_USER, id.toString(), username, passwordHash, displayName, 1,
                    now.toEpochMilli(), now.toEpochMilli());
            jdbcTemplate.update(INSERT_USER_ROLE, id.toString(),
                    jdbcTemplate.queryForObject(FIND_ROLE_ID, String.class, role));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateUsernameException();
        }
        return new UserAccount(id, username, passwordHash, displayName, true, now, now, List.of(role));
    }

    private UserAccount mapAccount(ResultSet resultSet, int row) throws SQLException {
        String role = resultSet.getString("role_name");
        return new UserAccount(UUID.fromString(resultSet.getString("id")), resultSet.getString("username"),
                resultSet.getString("password_hash"), resultSet.getString("display_name"),
                resultSet.getInt("enabled") != 0, Instant.ofEpochMilli(resultSet.getLong("created_at")),
                Instant.ofEpochMilli(resultSet.getLong("updated_at")), role == null ? List.of() : List.of(role));
    }

    /** Ошибка попытки повторной регистрации имени пользователя. */
    public static class DuplicateUsernameException extends RuntimeException {
        public DuplicateUsernameException() {
            super("Username is already registered");
        }
    }
}
