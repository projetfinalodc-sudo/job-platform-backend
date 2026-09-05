package com.odc.plateforme_emploi.exception;

/**
 * Levée lorsque la génération de contenu par IA échoue (clé API invalide,
 * quota dépassé, service IA indisponible, réponse inexploitable...).
 * Mappée en HTTP 502 par le GlobalExceptionHandler : ce n'est pas la faute
 * du candidat, et il peut toujours écrire sa lettre manuellement.
 */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message) {
        super(message);
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
