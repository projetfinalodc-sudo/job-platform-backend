package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.ApiResponse;
import com.odc.plateforme_emploi.dto.StatsPubliquesResponse;
import com.odc.plateforme_emploi.entity.Offre;
import com.odc.plateforme_emploi.repository.CandidatRepository;
import com.odc.plateforme_emploi.repository.OffreRepository;
import com.odc.plateforme_emploi.repository.RecruteurRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Endpoints publics, sans authentification (page d'accueil)")
public class StatsPubliquesController {

    private final OffreRepository offreRepository;
    private final CandidatRepository candidatRepository;
    private final RecruteurRepository recruteurRepository;

    @Operation(summary = "Statistiques agrégées pour la page d'accueil (public)")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsPubliquesResponse>> getStats() {
        StatsPubliquesResponse stats = new StatsPubliquesResponse(
            offreRepository.countByStatut(Offre.StatutOffre.ACTIVE),
            candidatRepository.count(),
            recruteurRepository.count()
        );
        return ResponseEntity.ok(ApiResponse.success("Statistiques publiques", stats));
    }
}
