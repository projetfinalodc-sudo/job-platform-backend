package com.odc.plateforme_emploi.exception;

/**
 * Levée lorsqu'une IP dépasse le nombre de tentatives autorisées sur une route
 * sensible (login, inscription, mot de passe oublié...) durant la fenêtre de temps
 * configurée. Interceptée directement par le filtre de rate limiting (avant même
 * d'atteindre les contrôleurs), donc jamais par le GlobalExceptionHandler.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
