-- liquibase formatted sql

-- changeset kulikov-mv:20260804-004.security-user-roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id TEXT NOT NULL,
    role_id TEXT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
-- rollback DROP TABLE IF EXISTS user_roles;

-- changeset kulikov-mv:20260804-005.security-role-index
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
-- rollback DROP INDEX IF EXISTS idx_user_roles_role_id;
