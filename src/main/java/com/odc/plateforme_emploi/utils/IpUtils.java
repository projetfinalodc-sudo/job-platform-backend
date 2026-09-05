package com.odc.plateforme_emploi.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extraction de l'adresse IP réelle du client, en tenant compte d'un éventuel
 * reverse proxy (Nginx, load balancer...) qui transmet l'IP d'origine via
 * l'en-tête X-Forwarded-For plutôt que request.getRemoteAddr() (qui, derrière
 * un proxy, renverrait l'IP du proxy et non celle du client).
 */
public final class IpUtils {

    private IpUtils() {
    }

    public static String extraireIpClient(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Peut contenir une chaîne "client, proxy1, proxy2" : le premier est le client d'origine.
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
