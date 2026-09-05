-- ============================================================
-- Migration : Refresh tokens + réinitialisation de mot de passe
-- ============================================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    utilisateur_id BIGINT NOT NULL,
    date_creation DATETIME NOT NULL,
    date_expiration DATETIME NOT NULL,
    revoque BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_token_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
        ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    utilisateur_id BIGINT NOT NULL,
    date_expiration DATETIME NOT NULL,
    CONSTRAINT fk_password_reset_token_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
        ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens(token);
