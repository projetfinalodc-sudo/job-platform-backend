-- ============================================================
-- Migration : Entretiens d'embauche
-- ============================================================

CREATE TABLE IF NOT EXISTS entretiens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidature_id BIGINT NOT NULL UNIQUE,
    date_heure DATETIME NOT NULL,
    modalite VARCHAR(20) NOT NULL,
    lieu_ou_lien VARCHAR(500),
    message TEXT,
    statut VARCHAR(20) NOT NULL DEFAULT 'PROPOSE',
    date_creation DATETIME NOT NULL,
    date_modification DATETIME,
    CONSTRAINT fk_entretien_candidature
        FOREIGN KEY (candidature_id) REFERENCES candidatures(id)
        ON DELETE CASCADE
);
