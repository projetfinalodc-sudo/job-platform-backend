package com.odc.plateforme_emploi.service;

import com.odc.plateforme_emploi.dto.EntretienResponse;
import com.odc.plateforme_emploi.dto.ProposerEntretienRequest;
import com.odc.plateforme_emploi.entity.Candidature;
import com.odc.plateforme_emploi.entity.Entretien;
import com.odc.plateforme_emploi.exception.BadRequestException;
import com.odc.plateforme_emploi.exception.ResourceNotFoundException;
import com.odc.plateforme_emploi.exception.UnauthorizedOperationException;
import com.odc.plateforme_emploi.mail.EmailService;
import com.odc.plateforme_emploi.mapper.EntretienMapper;
import com.odc.plateforme_emploi.notification.NotificationService;
import com.odc.plateforme_emploi.repository.CandidatureRepository;
import com.odc.plateforme_emploi.repository.EntretienRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comme dans un vrai processus de recrutement : une fois la candidature
 * ACCEPTEE, le recruteur propose un entretien (date, modalité, lieu/lien),
 * le candidat le confirme ou le décline, et chacun est notifié par e-mail +
 * notification interne à chaque étape.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntretienService {

    private final EntretienRepository entretienRepository;
    private final CandidatureRepository candidatureRepository;
    private final EntretienMapper entretienMapper;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional
    public EntretienResponse proposerEntretien(Long candidatureId, ProposerEntretienRequest request,
                                                String emailRecruteur) {
        Candidature candidature = trouverCandidature(candidatureId);
        verifierProprietaireOffre(candidature, emailRecruteur);

        if (candidature.getStatut() != Candidature.StatutCandidature.ACCEPTEE) {
            throw new BadRequestException(
                "Un entretien ne peut être proposé que pour une candidature acceptée."
            );
        }

        Entretien entretien = entretienRepository.findByCandidatureId(candidatureId)
            .orElseGet(Entretien::new);
        boolean reprogrammation = entretien.getId() != null;

        entretien.setCandidature(candidature);
        entretien.setDateHeure(request.getDateHeure());
        entretien.setModalite(request.getModalite());
        entretien.setLieuOuLien(request.getLieuOuLien());
        entretien.setMessage(request.getMessage());
        entretien.setStatut(Entretien.StatutEntretien.PROPOSE);
        entretien.setDateModification(LocalDateTime.now());

        Entretien saved = entretienRepository.save(entretien);
        log.info("Entretien {} pour candidature #{} : {}", reprogrammation ? "reprogrammé" : "proposé",
            candidatureId, saved.getDateHeure());

        try {
            emailService.envoyerInvitationEntretien(
                candidature.getCandidat().getEmail(),
                candidature.getCandidat().getPrenom(),
                candidature.getOffre().getTitre(),
                candidature.getOffre().getRecruteur().getEntreprise(),
                formaterDateHeure(saved.getDateHeure()),
                libelleModalite(saved.getModalite()),
                saved.getLieuOuLien(),
                saved.getMessage()
            );
        } catch (Exception e) {
            log.warn("Entretien #{} enregistré mais notification candidat échouée", saved.getId(), e);
        }

        notificationService.creerNotification(
            candidature.getCandidat(),
            reprogrammation ? "Entretien reprogrammé" : "Entretien proposé",
            "Un entretien vous a été proposé pour l'offre « " + candidature.getOffre().getTitre() + " ». "
                + "Merci de le confirmer ou de le décliner.",
            "/mes-candidatures",
            "ENTRETIEN_PROPOSE"
        );

        return entretienMapper.toResponse(saved);
    }

    public EntretienResponse getEntretien(Long candidatureId, String emailUtilisateur) {
        Candidature candidature = trouverCandidature(candidatureId);
        verifierAcces(candidature, emailUtilisateur);
        Entretien entretien = entretienRepository.findByCandidatureId(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Aucun entretien programmé pour cette candidature."));
        return entretienMapper.toResponse(entretien);
    }

    // Le candidat confirme ou décline l'entretien proposé
    @Transactional
    public EntretienResponse repondreEntretien(Long candidatureId, boolean accepte, String emailCandidat) {
        Candidature candidature = trouverCandidature(candidatureId);

        if (!candidature.getCandidat().getEmail().equals(emailCandidat)) {
            throw new UnauthorizedOperationException("Cette candidature ne vous appartient pas.");
        }

        Entretien entretien = entretienRepository.findByCandidatureId(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Aucun entretien programmé pour cette candidature."));

        if (entretien.getStatut() != Entretien.StatutEntretien.PROPOSE) {
            throw new BadRequestException("Cet entretien n'est plus en attente de réponse.");
        }

        entretien.setStatut(accepte ? Entretien.StatutEntretien.CONFIRME : Entretien.StatutEntretien.REFUSE);
        entretien.setDateModification(LocalDateTime.now());
        Entretien saved = entretienRepository.save(entretien);

        try {
            emailService.envoyerReponseEntretien(
                candidature.getOffre().getRecruteur().getEmail(),
                candidature.getOffre().getRecruteur().getPrenom(),
                candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom(),
                candidature.getOffre().getTitre(),
                accepte
            );
        } catch (Exception e) {
            log.warn("Réponse à l'entretien #{} enregistrée mais notification recruteur échouée", saved.getId(), e);
        }

        notificationService.creerNotification(
            candidature.getOffre().getRecruteur(),
            accepte ? "Entretien confirmé" : "Entretien décliné",
            candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom()
                + (accepte ? " a confirmé " : " a décliné ") + "l'entretien pour « "
                + candidature.getOffre().getTitre() + " ».",
            "/mes-offres",
            accepte ? "ENTRETIEN_CONFIRME" : "ENTRETIEN_REFUSE"
        );

        return entretienMapper.toResponse(saved);
    }

    @Transactional
    public void annulerEntretien(Long candidatureId, String emailRecruteur) {
        Candidature candidature = trouverCandidature(candidatureId);
        verifierProprietaireOffre(candidature, emailRecruteur);

        Entretien entretien = entretienRepository.findByCandidatureId(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Aucun entretien programmé pour cette candidature."));

        entretien.setStatut(Entretien.StatutEntretien.ANNULE);
        entretien.setDateModification(LocalDateTime.now());
        entretienRepository.save(entretien);

        try {
            emailService.envoyerAnnulationEntretien(
                candidature.getCandidat().getEmail(),
                candidature.getCandidat().getPrenom(),
                candidature.getOffre().getTitre(),
                candidature.getOffre().getRecruteur().getEntreprise()
            );
        } catch (Exception e) {
            log.warn("Entretien #{} annulé mais notification candidat échouée", entretien.getId(), e);
        }

        notificationService.creerNotification(
            candidature.getCandidat(),
            "Entretien annulé",
            "L'entretien prévu pour l'offre « " + candidature.getOffre().getTitre() + " » a été annulé par le recruteur.",
            "/mes-candidatures",
            "ENTRETIEN_ANNULE"
        );

        log.info("Entretien annulé pour candidature #{}", candidatureId);
    }

    // Entretiens à venir du candidat connecté (pour son tableau de bord)
    public List<EntretienResponse> getMesEntretiens(String emailCandidat) {
        return entretienRepository.findByCandidature_Candidat_EmailOrderByDateHeureAsc(emailCandidat)
            .stream().map(entretienMapper::toResponse).collect(Collectors.toList());
    }

    private Candidature trouverCandidature(Long id) {
        return candidatureRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée !"));
    }

    private void verifierProprietaireOffre(Candidature candidature, String emailRecruteur) {
        if (!candidature.getOffre().getRecruteur().getEmail().equals(emailRecruteur)) {
            throw new UnauthorizedOperationException(
                "Vous ne pouvez gérer que les entretiens de vos propres offres."
            );
        }
    }

    private void verifierAcces(Candidature candidature, String emailUtilisateur) {
        boolean estLeCandidat = candidature.getCandidat().getEmail().equals(emailUtilisateur);
        boolean estLeRecruteur = candidature.getOffre().getRecruteur().getEmail().equals(emailUtilisateur);
        if (!estLeCandidat && !estLeRecruteur) {
            throw new UnauthorizedOperationException("Vous n'avez pas accès à cet entretien.");
        }
    }

    private String formaterDateHeure(LocalDateTime dateHeure) {
        return dateHeure.format(
            java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", java.util.Locale.FRENCH)
        );
    }

    private String libelleModalite(Entretien.Modalite modalite) {
        return switch (modalite) {
            case PRESENTIEL -> "En présentiel";
            case VISIO -> "Visioconférence";
            case TELEPHONE -> "Téléphone";
        };
    }
}
