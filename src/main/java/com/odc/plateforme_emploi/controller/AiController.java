package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.ai.AiLetterService;
import com.odc.plateforme_emploi.dto.ApiResponse;
import com.odc.plateforme_emploi.dto.GenererLettreRequest;
import com.odc.plateforme_emploi.dto.GenererLettreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDAT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Intelligence Artificielle", description = "Génération assistée de lettres de motivation")
public class AiController {

    private final AiLetterService aiLetterService;

    @Operation(
        summary = "Générer un brouillon de lettre de motivation",
        description = "Utilise le profil du candidat et l'offre visée pour proposer un brouillon, "
            + "modifiable avant l'envoi de la candidature."
    )
    @PostMapping("/lettre-motivation")
    public ResponseEntity<ApiResponse<GenererLettreResponse>> genererLettre(
            @Valid @RequestBody GenererLettreRequest request,
            Authentication authentication) {
        String lettre = aiLetterService.genererLettrePourOffre(request.getOffreId(), authentication.getName());
        return ResponseEntity.ok(
            ApiResponse.success("Lettre générée ! Vous pouvez la modifier avant de l'envoyer.",
                new GenererLettreResponse(lettre))
        );
    }
}
