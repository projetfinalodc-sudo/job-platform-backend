package com.odc.plateforme_emploi.mapper;

import com.odc.plateforme_emploi.dto.EntretienResponse;
import com.odc.plateforme_emploi.entity.Entretien;
import org.springframework.stereotype.Component;

@Component
public class EntretienMapper {

    public EntretienResponse toResponse(Entretien e) {
        EntretienResponse response = new EntretienResponse();
        response.setId(e.getId());
        response.setCandidatureId(e.getCandidature().getId());
        response.setDateHeure(e.getDateHeure());
        response.setModalite(e.getModalite());
        response.setLieuOuLien(e.getLieuOuLien());
        response.setMessage(e.getMessage());
        response.setStatut(e.getStatut());
        response.setDateCreation(e.getDateCreation());
        return response;
    }
}
