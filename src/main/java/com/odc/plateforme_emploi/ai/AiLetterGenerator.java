package com.odc.plateforme_emploi.ai;

/**
 * Contrat commun à tout fournisseur de génération de texte par IA.
 *
 * Pour changer de fournisseur (ex : passer d'OpenAI à Claude), il suffit de
 * créer une nouvelle classe implémentant cette interface (ex: ClaudeLetterGenerator)
 * et de basculer la propriété app.ai.provider — rien d'autre dans l'application
 * n'a besoin de changer, ni le contrôleur, ni le service d'orchestration.
 */
public interface AiLetterGenerator {

    String genererLettreMotivation(LetterGenerationContext contexte);
}
