-- ============================================================
-- Migration : Notifications internes
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL,
    titre VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    lien VARCHAR(255) NULL,
    type VARCHAR(50) NOT NULL,
    lu BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation DATETIME NOT NULL,
    CONSTRAINT fk_notification_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
        ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_notifications_utilisateur ON notifications(utilisateur_id);
