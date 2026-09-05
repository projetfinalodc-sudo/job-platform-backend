package com.odc.plateforme_emploi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Active le support des méthodes @Async (ex: NotificationService.notifierNouvelleOffre)
 * qui s'exécutent sur un thread séparé du thread de la requête HTTP appelante.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
