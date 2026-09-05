package com.odc.plateforme_emploi.dto;

import com.odc.plateforme_emploi.entity.Entretien;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EntretienResponse {
    private Long id;
    private Long candidatureId;
    private LocalDateTime dateHeure;
    private Entretien.Modalite modalite;
    private String lieuOuLien;
    private String message;
    private Entretien.StatutEntretien statut;
    private LocalDateTime dateCreation;
}
