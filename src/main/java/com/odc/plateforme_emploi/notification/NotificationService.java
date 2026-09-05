package com.odc.plateforme_emploi.notification;

import com.odc.plateforme_emploi.dto.NotificationResponse;
import com.odc.plateforme_emploi.entity.Candidat;
import com.odc.plateforme_emploi.entity.Notification;
import com.odc.plateforme_emploi.entity.Offre;
import com.odc.plateforme_emploi.entity.Utilisateur;
import com.odc.plateforme_emploi.exception.ResourceNotFoundException;
import com.odc.plateforme_emploi.exception.UnauthorizedOperationException;
import com.odc.plateforme_emploi.mail.EmailService;
import com.odc.plateforme_emploi.repository.CandidatRepository;
import com.odc.plateforme_emploi.repository.NotificationRepository;
import com.odc.plateforme_emploi.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CandidatRepository candidatRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    // Mes notifications, les plus récentes en premier
    public List<NotificationResponse> getMesNotifications(String email) {
        Utilisateur utilisateur = trouverUtilisateur(email);
        return notificationRepository.findByUtilisateurIdOrderByDateCreationDesc(utilisateur.getId())
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long compterNonLues(String email) {
        Utilisateur utilisateur = trouverUtilisateur(email);
        return notificationRepository.countByUtilisateurIdAndLuFalse(utilisateur.getId());
    }

    public NotificationResponse marquerCommeLue(Long notificationId, String email) {
        Notification n = trouverEtVerifierPropriete(notificationId, email);
        n.setLu(true);
        return toResponse(notificationRepository.save(n));
    }

    public void marquerToutesCommeLues(String email) {
        Utilisateur utilisateur = trouverUtilisateur(email);
        List<Notification> nonLues = notificationRepository.findByUtilisateurIdAndLuFalse(utilisateur.getId());
        nonLues.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(nonLues);
    }

    public void supprimer(Long notificationId, String email) {
        Notification n = trouverEtVerifierPropriete(notificationId, email);
        notificationRepository.delete(n);
    }

    // Créer une notification interne pour un utilisateur précis (utilisé par les
    // autres services : nouvelle candidature reçue, réponse à une candidature...)
    public void creerNotification(Utilisateur destinataire, String titre, String message,
                                   String lien, String type) {
        Notification n = new Notification();
        n.setUtilisateur(destinataire);
        n.setTitre(titre);
        n.setMessage(message);
        n.setLien(lien);
        n.setType(type);
        notificationRepository.save(n);
    }

    /**
     * Diffuse une notification "nouvelle offre" à tous les candidats actifs et
     * ayant confirmé leur e-mail (notification interne + e-mail).
     *
     * Exécuté en tâche asynchrone (@Async) : sans ça, publier une offre attendrait
     * l'envoi de N e-mails avant de répondre au recruteur, ce qui rendrait le
     * bouton "Publier" perceptiblement lent dès que la base de candidats grandit.
     */
    @Async
    public void notifierNouvelleOffre(Offre offre) {
        List<Candidat> candidatsActifs = candidatRepository.findByActifTrueAndEnabledTrue();
        log.info("Diffusion de la nouvelle offre #{} à {} candidat(s)", offre.getId(), candidatsActifs.size());

        String titre = "Nouvelle offre : " + offre.getTitre();
        String message = offre.getRecruteur().getEntreprise() + " recrute pour le poste de "
            + offre.getTitre() + " à " + offre.getLocalisation() + ".";
        String lien = "/offres/" + offre.getId();

        for (Candidat candidat : candidatsActifs) {
            try {
                creerNotification(candidat, titre, message, lien, "NOUVELLE_OFFRE");
                emailService.envoyerNotificationNouvelleOffre(
                    candidat.getEmail(), candidat.getPrenom(), offre.getTitre(),
                    offre.getRecruteur().getEntreprise(), offre.getLocalisation(), offre.getId()
                );
            } catch (Exception e) {
                // Un échec (email invalide, SMTP indisponible...) pour un candidat ne doit
                // jamais interrompre la diffusion aux autres candidats.
                log.warn("Échec de notification pour le candidat {}", candidat.getEmail(), e);
            }
        }
    }

    private Utilisateur trouverUtilisateur(String email) {
        return utilisateurRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé !"));
    }

    private Notification trouverEtVerifierPropriete(Long notificationId, String email) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée !"));

        if (!n.getUtilisateur().getEmail().equals(email)) {
            throw new UnauthorizedOperationException("Cette notification ne vous appartient pas.");
        }
        return n;
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setTitre(n.getTitre());
        r.setMessage(n.getMessage());
        r.setLien(n.getLien());
        r.setType(n.getType());
        r.setLu(n.isLu());
        r.setDateCreation(n.getDateCreation());
        return r;
    }
}
