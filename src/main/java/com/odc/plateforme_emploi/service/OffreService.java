package com.odc.plateforme_emploi.service;

import com.odc.plateforme_emploi.dto.OffreRequest;
import com.odc.plateforme_emploi.dto.OffreResponse;
import com.odc.plateforme_emploi.entity.Offre;
import com.odc.plateforme_emploi.entity.Recruteur;
import com.odc.plateforme_emploi.exception.ResourceNotFoundException;
import com.odc.plateforme_emploi.exception.UnauthorizedOperationException;
import com.odc.plateforme_emploi.notification.NotificationService;
import com.odc.plateforme_emploi.mapper.OffreMapper;
import com.odc.plateforme_emploi.repository.OffreRepository;
import com.odc.plateforme_emploi.repository.RecruteurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OffreService {

    private final OffreRepository offreRepository;
    private final RecruteurRepository recruteurRepository;
    private final NotificationService notificationService;
    private final OffreMapper offreMapper;

    // Créer une offre
    public OffreResponse creerOffre(OffreRequest request, String emailRecruteur) {
        Recruteur recruteur = recruteurRepository.findByEmail(emailRecruteur)
            .orElseThrow(() -> new ResourceNotFoundException("Recruteur non trouvé !"));

        Offre offre = new Offre();
        offre.setTitre(request.getTitre());
        offre.setDescription(request.getDescription());
        offre.setLocalisation(request.getLocalisation());
        offre.setTypeContrat(request.getTypeContrat());
        offre.setNiveauEtude(request.getNiveauEtude());
        offre.setSalaire(request.getSalaire());
        offre.setDateExpiration(request.getDateExpiration());
        offre.setRecruteur(recruteur);

        Offre saved = offreRepository.save(offre);

        // Une offre est active dès sa création (voir Offre.statut) : la diffusion
        // à tous les candidats actifs part donc immédiatement, en tâche de fond.
        notificationService.notifierNouvelleOffre(saved);

        return offreMapper.toResponse(saved);
    }

    // Toutes les offres actives
    public List<OffreResponse> getToutesLesOffres() {
        return offreMapper.toResponses(
            offreRepository.findByStatut(Offre.StatutOffre.ACTIVE)
        );
    }

    // Une offre par ID
    public OffreResponse getOffreById(Long id) {
        Offre offre = offreRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée !"));
        return offreMapper.toResponse(offre);
    }

    // Offres d'un recruteur
    public List<OffreResponse> getOffresRecruteur(String emailRecruteur) {
        Recruteur recruteur = recruteurRepository.findByEmail(emailRecruteur)
            .orElseThrow(() -> new ResourceNotFoundException("Recruteur non trouvé !"));
        return offreMapper.toResponses(offreRepository.findByRecruteurId(recruteur.getId()));
    }

    // Recherche par titre ou localisation
    public List<OffreResponse> rechercherOffres(String keyword) {
        return offreMapper.toResponses(
            offreRepository.findByTitreContainingIgnoreCaseOrLocalisationContainingIgnoreCase(
                keyword, keyword)
        );
    }

    // Modifier une offre
    public OffreResponse modifierOffre(Long id, OffreRequest request,
                                        String emailRecruteur) {
        Offre offre = offreRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée !"));

        if (!offre.getRecruteur().getEmail().equals(emailRecruteur)) {
            throw new UnauthorizedOperationException("Vous n'êtes pas autorisé à modifier cette offre.");
        }

        offre.setTitre(request.getTitre());
        offre.setDescription(request.getDescription());
        offre.setLocalisation(request.getLocalisation());
        offre.setTypeContrat(request.getTypeContrat());
        offre.setNiveauEtude(request.getNiveauEtude());
        offre.setSalaire(request.getSalaire());
        offre.setDateExpiration(request.getDateExpiration());

        return offreMapper.toResponse(offreRepository.save(offre));
    }

    // Supprimer une offre
    public void supprimerOffre(Long id, String emailRecruteur) {
        Offre offre = offreRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée !"));

        if (!offre.getRecruteur().getEmail().equals(emailRecruteur)) {
            throw new UnauthorizedOperationException("Vous n'êtes pas autorisé à modifier cette offre.");
        }
        offreRepository.delete(offre);
    }

    // Conservé pour compatibilité (utilisé par DashboardService) — délègue au mapper.
    public List<OffreResponse> toResponses(List<Offre> offres) {
        return offreMapper.toResponses(offres);
    }
}
