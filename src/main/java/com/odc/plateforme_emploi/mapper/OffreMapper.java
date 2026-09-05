package com.odc.plateforme_emploi.mapper;

import com.odc.plateforme_emploi.dto.OffreResponse;
import com.odc.plateforme_emploi.entity.Offre;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OffreMapper {

    public OffreResponse toResponse(Offre offre) {
        OffreResponse response = new OffreResponse();
        response.setId(offre.getId());
        response.setTitre(offre.getTitre());
        response.setDescription(offre.getDescription());
        response.setLocalisation(offre.getLocalisation());
        response.setTypeContrat(offre.getTypeContrat());
        response.setNiveauEtude(offre.getNiveauEtude());
        response.setSalaire(offre.getSalaire());
        response.setStatut(offre.getStatut());
        response.setDatePublication(offre.getDatePublication());
        response.setDateExpiration(offre.getDateExpiration());
        response.setRecruteurId(offre.getRecruteur().getId());
        response.setEntreprise(offre.getRecruteur().getEntreprise());
        response.setSecteur(offre.getRecruteur().getSecteur());
        response.setNombreCandidatures(
            offre.getCandidatures() != null ? offre.getCandidatures().size() : 0
        );
        return response;
    }

    public List<OffreResponse> toResponses(List<Offre> offres) {
        return offres.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
