package com.odc.plateforme_emploi.dto;

import com.odc.plateforme_emploi.entity.Entretien;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProposerEntretienRequest {

    @NotNull(message = "La date et l'heure de l'entretien sont obligatoires")
    @Future(message = "La date de l'entretien doit être dans le futur")
    private LocalDateTime dateHeure;

    @NotNull(message = "La modalité est obligatoire")
    private Entretien.Modalite modalite;

    // Adresse (PRESENTIEL), lien de visio (VISIO) ou numéro (TELEPHONE)
    private String lieuOuLien;

    private String message;
}
