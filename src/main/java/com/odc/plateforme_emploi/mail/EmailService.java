package com.odc.plateforme_emploi.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private final RestClient restClient = RestClient.create();

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.brevo.api-key}")
    private String brevoApiKey;

    public void envoyerEmailActivation(String destinataire, String prenom, String token) {
        String lienActivation = frontendUrl + "/activation?token=" + token;
        String sujet = "Activez votre compte JobPlatform.GN";
        String contenuHtml = construireTemplateActivation(prenom, lienActivation);

        envoyer(destinataire, sujet, contenuHtml);
    }

    public void envoyerNotificationNouvelleCandidature(String destinataireRecruteur,
                                                         String prenomRecruteur,
                                                         String nomCompletCandidat,
                                                         String titreOffre) {
        String lienDashboard = frontendUrl + "/recruteur/offres";
        String sujet = "Nouvelle candidature reçue — " + titreOffre;
        String contenuHtml = construireTemplateNouvelleCandidature(
            prenomRecruteur, nomCompletCandidat, titreOffre, lienDashboard
        );

        envoyer(destinataireRecruteur, sujet, contenuHtml);
    }

    public void envoyerNotificationChangementStatut(String destinataireCandidat,
                                                      String prenomCandidat,
                                                      String titreOffre,
                                                      String entreprise,
                                                      boolean accepte) {
        String lienCandidatures = frontendUrl + "/mes-candidatures";
        String sujet = accepte
            ? "Bonne nouvelle pour votre candidature — " + titreOffre
            : "Réponse à votre candidature — " + titreOffre;
        String contenuHtml = construireTemplateChangementStatut(
            prenomCandidat, titreOffre, entreprise, accepte, lienCandidatures
        );

        envoyer(destinataireCandidat, sujet, contenuHtml);
    }

    public void envoyerEmailReinitialisationMotDePasse(String destinataire, String prenom, String token) {
        String lienReset = frontendUrl + "/reset-password?token=" + token;
        String sujet = "Réinitialisation de votre mot de passe — JobPlatform.GN";
        String contenuHtml = construireTemplateResetPassword(prenom, lienReset);

        envoyer(destinataire, sujet, contenuHtml);
    }

    public void envoyerNotificationNouvelleOffre(String destinataireCandidat,
                                                   String prenomCandidat,
                                                   String titreOffre,
                                                   String entreprise,
                                                   String localisation,
                                                   Long offreId) {
        String lienOffre = frontendUrl + "/offres/" + offreId;
        String sujet = "Nouvelle offre publiée sur JobPlatform.GN";
        String contenuHtml = construireTemplateNouvelleOffre(
            prenomCandidat, titreOffre, entreprise, localisation, lienOffre
        );

        envoyer(destinataireCandidat, sujet, contenuHtml);
    }

    public void envoyerInvitationEntretien(String destinataireCandidat,
                                            String prenomCandidat,
                                            String titreOffre,
                                            String entreprise,
                                            String dateHeureFormatee,
                                            String modaliteLabel,
                                            String lieuOuLien,
                                            String messageRecruteur) {
        String lienCandidatures = frontendUrl + "/mes-candidatures";
        String sujet = "Invitation à un entretien — " + titreOffre;
        String contenuHtml = construireTemplateInvitationEntretien(
            prenomCandidat, titreOffre, entreprise, dateHeureFormatee,
            modaliteLabel, lieuOuLien, messageRecruteur, lienCandidatures
        );

        envoyer(destinataireCandidat, sujet, contenuHtml);
    }

    public void envoyerReponseEntretien(String destinataireRecruteur,
                                         String prenomRecruteur,
                                         String nomCompletCandidat,
                                         String titreOffre,
                                         boolean confirme) {
        String lienOffres = frontendUrl + "/mes-offres";
        String sujet = confirme
            ? "Entretien confirmé par le candidat — " + titreOffre
            : "Entretien décliné par le candidat — " + titreOffre;
        String contenuHtml = construireTemplateReponseEntretien(
            prenomRecruteur, nomCompletCandidat, titreOffre, confirme, lienOffres
        );

        envoyer(destinataireRecruteur, sujet, contenuHtml);
    }

    public void envoyerAnnulationEntretien(String destinataireCandidat,
                                            String prenomCandidat,
                                            String titreOffre,
                                            String entreprise) {
        String lienCandidatures = frontendUrl + "/mes-candidatures";
        String sujet = "Entretien annulé — " + titreOffre;
        String contenuHtml = construireTemplateAnnulationEntretien(
            prenomCandidat, titreOffre, entreprise, lienCandidatures
        );

        envoyer(destinataireCandidat, sujet, contenuHtml);
    }

    private void envoyer(String destinataire, String sujet, String contenuHtml) {
        if (brevoApiKey == null || brevoApiKey.isBlank() || brevoApiKey.startsWith("TA_CLE")) {
            log.error("Envoi d'e-mail annulé : clé API Brevo non configurée (app.brevo.api-key).");
            throw new RuntimeException("Le service d'envoi d'e-mail n'est pas configuré.");
        }

        Map<String, Object> corps = Map.of(
            "sender", Map.of("name", fromName, "email", fromAddress),
            "to", List.of(Map.of("email", destinataire)),
            "subject", sujet,
            "htmlContent", contenuHtml
        );

        try {
            String reponse = restClient.post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", brevoApiKey)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corps)
                .retrieve()
                .body(String.class);

            log.info("E-mail envoyé à {} via Brevo : {} — réponse : {}", destinataire, sujet, reponse);

        } catch (org.springframework.web.client.RestClientResponseException e) {
            // Le corps de la réponse contient la raison précise (expéditeur non
            // vérifié, clé invalide, quota dépassé...) — précieux pour diagnostiquer.
            log.error("Brevo a refusé l'envoi à {} — HTTP {} : {}",
                destinataire, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new RuntimeException("Impossible d'envoyer l'e-mail. Merci de réessayer plus tard.");
        } catch (RestClientException e) {
            log.error("Échec de l'envoi de l'e-mail à {} (réseau/timeout)", destinataire, e);
            throw new RuntimeException("Impossible d'envoyer l'e-mail. Merci de réessayer plus tard.");
        }
    }

    private String construireTemplateActivation(String prenom, String lienActivation) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:#0f766e; padding:24px 32px;">
                          <span style="color:#ffffff; font-size:20px; font-weight:bold;">JobPlatform.GN</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="color:#111827; margin-top:0;">Bienvenue %s !</h2>
                          <p style="color:#374151; font-size:15px; line-height:1.6;">
                            Merci de vous être inscrit sur JobPlatform.GN. Pour activer votre compte
                            et commencer à utiliser la plateforme, cliquez sur le bouton ci-dessous :
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                            <tr>
                              <td style="border-radius:6px; background-color:#0f766e;">
                                <a href="%s" style="display:inline-block; padding:12px 28px; color:#ffffff; text-decoration:none; font-weight:bold; font-size:15px;">
                                  Activer mon compte
                                </a>
                              </td>
                            </tr>
                          </table>
                          <p style="color:#6b7280; font-size:13px; line-height:1.6;">
                            Ce lien est valable 24 heures. Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>
                            <a href="%s" style="color:#0f766e; word-break:break-all;">%s</a>
                          </p>
                          <p style="color:#9ca3af; font-size:12px; margin-top:32px;">
                            Si vous n'êtes pas à l'origine de cette inscription, ignorez simplement cet e-mail.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(prenom, lienActivation, lienActivation, lienActivation);
    }

    private String construireTemplateNouvelleCandidature(String prenomRecruteur,
                                                           String nomCompletCandidat,
                                                           String titreOffre,
                                                           String lienDashboard) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:#0f766e; padding:24px 32px;">
                          <span style="color:#ffffff; font-size:20px; font-weight:bold;">JobPlatform.GN</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="color:#111827; margin-top:0;">Nouvelle candidature reçue</h2>
                          <p style="color:#374151; font-size:15px; line-height:1.6;">
                            Bonjour %s,<br><br>
                            <strong>%s</strong> vient de postuler à votre offre
                            <strong>« %s »</strong>.
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                            <tr>
                              <td style="border-radius:6px; background-color:#0f766e;">
                                <a href="%s" style="display:inline-block; padding:12px 28px; color:#ffffff; text-decoration:none; font-weight:bold; font-size:15px;">
                                  Voir la candidature
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(prenomRecruteur, nomCompletCandidat, titreOffre, lienDashboard);
    }

    private String construireTemplateChangementStatut(String prenomCandidat,
                                                        String titreOffre,
                                                        String entreprise,
                                                        boolean accepte,
                                                        String lienCandidatures) {
        String couleur = accepte ? "#057642" : "#0f766e";
        String titre = accepte ? "Votre candidature a été retenue 🎉" : "Mise à jour de votre candidature";
        String corps = accepte
            ? "Nous avons le plaisir de vous informer que votre candidature au poste <strong>« " + titreOffre
              + " »</strong> chez <strong>" + entreprise + "</strong> a été retenue par le recruteur. "
              + "Celui-ci reviendra vers vous prochainement pour la suite du processus."
            : "Nous vous remercions pour l'intérêt porté au poste <strong>« " + titreOffre
              + " »</strong> chez <strong>" + entreprise + "</strong>. Après étude de votre profil, "
              + "le recruteur a choisi de ne pas donner suite à votre candidature pour ce poste. "
              + "N'hésitez pas à consulter nos autres offres.";

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:%s; padding:24px 32px;">
                          <span style="color:#ffffff; font-size:20px; font-weight:bold;">JobPlatform.GN</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="color:#111827; margin-top:0;">%s</h2>
                          <p style="color:#374151; font-size:15px; line-height:1.6;">
                            Bonjour %s,<br><br>%s
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                            <tr>
                              <td style="border-radius:6px; background-color:%s;">
                                <a href="%s" style="display:inline-block; padding:12px 28px; color:#ffffff; text-decoration:none; font-weight:bold; font-size:15px;">
                                  Voir mes candidatures
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(couleur, titre, prenomCandidat, corps, couleur, lienCandidatures);
    }

    private String construireTemplateResetPassword(String prenom, String lienReset) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:#0f766e; padding:24px 32px;">
                          <span style="color:#ffffff; font-size:20px; font-weight:bold;">JobPlatform.GN</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="color:#111827; margin-top:0;">Réinitialisation de mot de passe</h2>
                          <p style="color:#374151; font-size:15px; line-height:1.6;">
                            Bonjour %s,<br><br>
                            Vous avez demandé à réinitialiser votre mot de passe. Cliquez sur le bouton
                            ci-dessous pour en choisir un nouveau. Ce lien est valable 30 minutes.
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                            <tr>
                              <td style="border-radius:6px; background-color:#0f766e;">
                                <a href="%s" style="display:inline-block; padding:12px 28px; color:#ffffff; text-decoration:none; font-weight:bold; font-size:15px;">
                                  Réinitialiser mon mot de passe
                                </a>
                              </td>
                            </tr>
                          </table>
                          <p style="color:#9ca3af; font-size:12px; margin-top:32px;">
                            Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail —
                            votre mot de passe actuel reste inchangé.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(prenom, lienReset);
    }

    private String construireTemplateNouvelleOffre(String prenomCandidat, String titreOffre,
                                                     String entreprise, String localisation,
                                                     String lienOffre) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:#0f766e; padding:24px 32px;">
                          <span style="color:#ffffff; font-size:20px; font-weight:bold;">JobPlatform.GN</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="color:#111827; margin-top:0;">Nouvelle offre publiée</h2>
                          <p style="color:#374151; font-size:15px; line-height:1.6;">
                            Bonjour %s,<br><br>
                            <strong>%s</strong> recrute pour le poste de <strong>%s</strong> à %s.
                            Cette offre pourrait vous intéresser !
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                            <tr>
                              <td style="border-radius:6px; background-color:#0f766e;">
                                <a href="%s" style="display:inline-block; padding:12px 28px; color:#ffffff; text-decoration:none; font-weight:bold; font-size:15px;">
                                  Voir l'offre
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(prenomCandidat, entreprise, titreOffre, localisation, lienOffre);
    }

    private String construireTemplateInvitationEntretien(String prenomCandidat,
                                                           String titreOffre,
                                                           String entreprise,
                                                           String dateHeureFormatee,
                                                           String modaliteLabel,
                                                           String lieuOuLien,
                                                           String messageRecruteur,
                                                           String lienCandidatures) {
        String ligneMessage = (messageRecruteur == null || messageRecruteur.isBlank())
            ? ""
            : "<p style=\"color:#374151; font-size:14px; line-height:1.6; background:#f4f6f8; "
              + "padding:12px 16px; border-radius:8px; margin-top:16px;\"><em>« " + messageRecruteur + " »</em></p>";

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:#0f766e; padding:24px 32px;">
                          <span style="color:#ffffff; font-size:20px; font-weight:bold;">JobPlatform.GN</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="color:#111827; margin-top:0;">Vous êtes invité(e) à un entretien 📅</h2>
                          <p style="color:#374151; font-size:15px; line-height:1.6;">
                            Bonjour %s,<br><br>
                            <strong>%s</strong> souhaite vous rencontrer pour le poste
                            <strong>« %s »</strong>.
                          </p>
                          <table cellpadding="0" cellspacing="0" style="width:100%%; margin:20px 0; background:#f0fdf9; border-radius:8px;">
                            <tr><td style="padding:16px 20px; font-size:14px; color:#111827;">
                              <strong>Date :</strong> %s<br>
                              <strong>Modalité :</strong> %s<br>
                              <strong>%s :</strong> %s
                            </td></tr>
                          </table>
                          %s
                          <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                            <tr>
                              <td style="border-radius:6px; background-color:#0f766e;">
                                <a href="%s" style="display:inline-block; padding:12px 28px; color:#ffffff; text-decoration:none; font-weight:bold; font-size:15px;">
                                  Confirmer ou décliner
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                prenomCandidat, entreprise, titreOffre, dateHeureFormatee, modaliteLabel,
                modaliteLabel.equals("Présentiel") ? "Adresse" : modaliteLabel.equals("Visio") ? "Lien" : "Numéro",
                lieuOuLien != null ? lieuOuLien : "communiqué séparément",
                ligneMessage, lienCandidatures
            );
    }

    private String construireTemplateReponseEntretien(String prenomRecruteur,
                                                        String nomCompletCandidat,
                                                        String titreOffre,
                                                        boolean confirme,
                                                        String lienOffres) {
        String couleur = confirme ? "#057642" : "#cc1016";
        String titre = confirme ? "Entretien confirmé" : "Entretien décliné";
        String corps = confirme
            ? "<strong>" + nomCompletCandidat + "</strong> a confirmé sa présence à l'entretien pour le poste « "
              + titreOffre + " »."
            : "<strong>" + nomCompletCandidat + "</strong> a décliné l'entretien proposé pour le poste « "
              + titreOffre + " ». Vous pouvez lui reproposer un autre créneau si vous le souhaitez.";

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:%s; padding:24px 32px;">
                          <span style="color:#ffffff; font-size:20px; font-weight:bold;">JobPlatform.GN</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="color:#111827; margin-top:0;">%s</h2>
                          <p style="color:#374151; font-size:15px; line-height:1.6;">
                            Bonjour %s,<br><br>%s
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                            <tr>
                              <td style="border-radius:6px; background-color:%s;">
                                <a href="%s" style="display:inline-block; padding:12px 28px; color:#ffffff; text-decoration:none; font-weight:bold; font-size:15px;">
                                  Voir mes offres
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(couleur, titre, prenomRecruteur, corps, couleur, lienOffres);
    }

    private String construireTemplateAnnulationEntretien(String prenomCandidat,
                                                           String titreOffre,
                                                           String entreprise,
                                                           String lienCandidatures) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8; padding:32px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:#cc1016; padding:24px 32px;">
                          <span style="color:#ffffff; font-size:20px; font-weight:bold;">JobPlatform.GN</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="color:#111827; margin-top:0;">Entretien annulé</h2>
                          <p style="color:#374151; font-size:15px; line-height:1.6;">
                            Bonjour %s,<br><br>
                            <strong>%s</strong> a annulé l'entretien prévu pour le poste
                            <strong>« %s »</strong>. Consultez votre espace candidat pour plus de détails.
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:24px 0;">
                            <tr>
                              <td style="border-radius:6px; background-color:#0f766e;">
                                <a href="%s" style="display:inline-block; padding:12px 28px; color:#ffffff; text-decoration:none; font-weight:bold; font-size:15px;">
                                  Voir mes candidatures
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(prenomCandidat, entreprise, titreOffre, lienCandidatures);
    }
}
