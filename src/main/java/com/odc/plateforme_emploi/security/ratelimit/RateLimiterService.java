package com.odc.plateforme_emploi.security.ratelimit;

import com.odc.plateforme_emploi.exception.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limiteur de requêtes en mémoire, à fenêtre fixe, par clé arbitraire
 * (typiquement "ip:route"). Volontairement simple et sans dépendance externe
 * (type Redis/Bucket4j) : pour une application mono-instance comme celle-ci,
 * une Map en mémoire suffit et évite d'ajouter de l'infrastructure inutile.
 *
 * ⚠️ Limite connue : ce compteur est local à l'instance. Si l'application est
 * un jour déployée sur plusieurs instances derrière un load balancer, chaque
 * instance aura son propre compteur (la vraie limite globale serait alors
 * multipliée par le nombre d'instances). Pour ce projet, une seule instance
 * tourne, donc ce n'est pas un problème.
 */
@Slf4j
@Service
public class RateLimiterService {

    private static class Fenetre {
        final AtomicInteger compteur = new AtomicInteger(0);
        volatile long debut;

        Fenetre(long debut) {
            this.debut = debut;
        }
    }

    private final Map<String, Fenetre> fenetres = new ConcurrentHashMap<>();

    public void verifierLimite(String cle, LimiteConfig config) {
        long maintenant = System.currentTimeMillis();
        Fenetre fenetre = fenetres.computeIfAbsent(cle, k -> new Fenetre(maintenant));

        synchronized (fenetre) {
            if (maintenant - fenetre.debut > config.fenetreMs()) {
                // La fenêtre précédente est expirée : on repart à zéro.
                fenetre.debut = maintenant;
                fenetre.compteur.set(0);
            }

            int tentatives = fenetre.compteur.incrementAndGet();
            if (tentatives > config.maxTentatives()) {
                long tempsRestantMs = config.fenetreMs() - (maintenant - fenetre.debut);
                long minutesRestantes = Math.max(1, tempsRestantMs / 60_000);
                log.warn("Rate limit dépassé pour la clé [{}] : {} tentatives", cle, tentatives);
                throw new TooManyRequestsException(
                    "Trop de tentatives. Merci de réessayer dans " + minutesRestantes + " minute(s)."
                );
            }
        }
    }

    // Purge les fenêtres inactives depuis longtemps, pour éviter que la Map ne
    // grossisse indéfiniment avec des IP qui ne reviennent jamais.
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void nettoyer() {
        long maintenant = System.currentTimeMillis();
        int tailleAvant = fenetres.size();
        fenetres.entrySet().removeIf(e -> maintenant - e.getValue().debut > 60 * 60 * 1000);
        int purgees = tailleAvant - fenetres.size();
        if (purgees > 0) {
            log.debug("Rate limiter : {} fenêtre(s) inactive(s) purgée(s)", purgees);
        }
    }
}
