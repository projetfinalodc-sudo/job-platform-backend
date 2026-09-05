package com.odc.plateforme_emploi.ai;

/**
 * Données nécessaires pour générer une lettre de motivation, indépendantes de
 * tout fournisseur IA. Chaque implémentation de {@link AiLetterGenerator}
 * reçoit ce même contexte, quel que soit le prestataire utilisé derrière.
 */
public record LetterGenerationContext(
    String prenomCandidat,
    String nomCandidat,
    String competencesCandidat,
    String biographieCandidat,
    String titrePoste,
    String entreprise,
    String descriptionOffre
) {
}
