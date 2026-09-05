package com.odc.plateforme_emploi.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odc.plateforme_emploi.exception.AiGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Génère des lettres de motivation via l'API Chat Completions d'OpenAI (ChatGPT).
 *
 * Seule implémentation active tant que app.ai.provider=openai (valeur par défaut).
 * Pour basculer vers un autre fournisseur (ex: Claude), créer une classe similaire
 * avec @ConditionalOnProperty(havingValue = "claude") — le reste de l'application
 * (AiLetterService, AiController) n'a besoin d'aucune modification.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiLetterGenerator implements AiLetterGenerator {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.openai.api-key}")
    private String apiKey;

    @Value("${app.ai.openai.model}")
    private String model;

    public OpenAiLetterGenerator() {
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .requestFactory(clientHttpRequestFactoryAvecTimeout())
            .build();
    }

    private org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactoryAvecTimeout() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return factory;
    }

    @Override
    public String genererLettreMotivation(LetterGenerationContext contexte) {
        // Une vraie clé OpenAI commence toujours par "sk-" — vérifier le format
        // réel plutôt qu'un texte d'espace réservé précis évite tout faux positif
        // si un reste de placeholder traîne devant la clé collée.
        if (apiKey == null || apiKey.isBlank() || !apiKey.trim().startsWith("sk-")) {
            throw new AiGenerationException(
                "La génération par IA n'est pas configurée correctement (clé API OpenAI manquante ou invalide). "
                + "Vérifie que app.ai.openai.api-key ne contient QUE la clé, sans texte autour."
            );
        }

        String prompt = construirePrompt(contexte);

        Map<String, Object> corps = Map.of(
            "model", model,
            "messages", List.of(
                Map.of(
                    "role", "system",
                    "content", "Tu es un expert en rédaction de lettres de motivation "
                        + "professionnelles, concises et percutantes, en français."
                ),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7,
            "max_tokens", 700
        );

        try {
            String reponseBrute = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corps)
                .retrieve()
                .body(String.class);

            return extraireContenu(reponseBrute);

        } catch (org.springframework.web.client.RestClientResponseException e) {
            // OpenAI a répondu avec une erreur HTTP (401 clé invalide, 429 quota dépassé,
            // 404 modèle inconnu...) — le corps de la réponse contient la raison précise.
            log.error("OpenAI a refusé la requête — HTTP {} : {}",
                e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new AiGenerationException(
                "Le service de génération IA a refusé la requête (code " + e.getStatusCode().value()
                + "). Vérifiez la clé API et son quota dans application.properties. "
                + "Vous pouvez rédiger votre lettre manuellement.", e
            );
        } catch (RestClientException e) {
            log.error("Échec de l'appel à l'API OpenAI (réseau/timeout)", e);
            throw new AiGenerationException(
                "Le service de génération IA est momentanément indisponible (problème réseau). "
                + "Vous pouvez rédiger votre lettre manuellement.", e
            );
        }
    }

    private String extraireContenu(String reponseBrute) {
        try {
            JsonNode racine = objectMapper.readTree(reponseBrute);
            JsonNode contenu = racine.path("choices").path(0).path("message").path("content");
            if (contenu.isMissingNode() || contenu.asText().isBlank()) {
                throw new AiGenerationException("Réponse de l'IA inexploitable.");
            }
            return contenu.asText().trim();
        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Échec du parsing de la réponse OpenAI : {}", reponseBrute, e);
            throw new AiGenerationException("Réponse de l'IA inexploitable.", e);
        }
    }

    private String construirePrompt(LetterGenerationContext c) {
        return """
            Rédige une lettre de motivation professionnelle en français pour la candidature suivante.

            Candidat : %s %s
            Compétences : %s
            Profil / expérience : %s

            Poste visé : %s
            Entreprise : %s
            Description du poste : %s

            Consignes :
            - Environ 250 à 350 mots, structurée en 3-4 paragraphes.
            - Ton professionnel, sincère, sans formules toutes faites excessives.
            - Mets en valeur les compétences du candidat en lien direct avec le poste.
            - N'invente aucune expérience ou compétence qui ne serait pas mentionnée ci-dessus.
            - Termine par une formule de politesse classique.
            - Ne mets aucune balise ni aucun texte d'introduction avant/après la lettre : uniquement le texte de la lettre elle-même.
            """.formatted(
                c.prenomCandidat(), c.nomCandidat(),
                vide(c.competencesCandidat()), vide(c.biographieCandidat()),
                c.titrePoste(), c.entreprise(), vide(c.descriptionOffre())
            );
    }

    private String vide(String valeur) {
        return (valeur == null || valeur.isBlank()) ? "Non renseigné" : valeur;
    }
}
