package com.odc.plateforme_emploi.security.ratelimit;

/**
 * Configuration d'une limite de requêtes : au plus {@code maxTentatives}
 * requêtes par IP durant une fenêtre glissante de {@code fenetreMs} millisecondes.
 */
public record LimiteConfig(int maxTentatives, long fenetreMs) {
}
