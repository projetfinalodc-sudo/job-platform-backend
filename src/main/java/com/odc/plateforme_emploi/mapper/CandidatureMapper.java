package com.odc.plateforme_emploi.mapper;

import com.odc.plateforme_emploi.dto.CandidatureResponse;
import com.odc.plateforme_emploi.entity.Candidature;
import com.odc.plateforme_emploi.repository.EntretienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CandidatureMapper {

    private final EntretienRepository entretienRepository;
    private final EntretienMapper entretienMapper;

    public CandidatureResponse toResponse(Candidature c) {
        CandidatureResponse response = new CandidatureResponse();
        response.setId(c.getId());
        response.setStatut(c.getStatut());
        response.setDateCandidature(c.getDateCandidature());
        response.setLettreMotivation(c.getLettreMotivation());
        response.setLettreMotivationPath(c.getLettreMotivationPath());
        response.setCvPath(c.getCvPath());
        response.setTelephoneContact(c.getTelephoneContact());
        response.setDateDisponibilite(c.getDateDisponibilite());
        response.setCandidatId(c.getCandidat().getId());
        response.setNomCandidat(c.getCandidat().getNom());
        response.setPrenomCandidat(c.getCandidat().getPrenom());
        response.setEmailCandidat(c.getCandidat().getEmail());
        response.setOffreId(c.getOffre().getId());
        response.setTitreOffre(c.getOffre().getTitre());
        response.setEntreprise(c.getOffre().getRecruteur().getEntreprise());

        entretienRepository.findByCandidatureId(c.getId())
            .ifPresent(entretien -> response.setEntretien(entretienMapper.toResponse(entretien)));

        return response;
    }

    public List<CandidatureResponse> toResponses(List<Candidature> candidatures) {
        return candidatures.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
