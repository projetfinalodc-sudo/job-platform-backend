package com.odc.plateforme_emploi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenererLettreRequest {

    @NotNull(message = "L'id de l'offre est obligatoire")
    private Long offreId;
}
