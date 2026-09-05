package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.ApiResponse;
import com.odc.plateforme_emploi.dto.DashboardCandidatResponse;
import com.odc.plateforme_emploi.dto.DashboardRecruteurResponse;
import com.odc.plateforme_emploi.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboards", description = "Statistiques agrégées pour les tableaux de bord candidat/recruteur")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Statistiques du tableau de bord candidat")
    @GetMapping("/candidat")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApiResponse<DashboardCandidatResponse>> getDashboardCandidat(
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Dashboard récupéré",
                dashboardService.getDashboardCandidat(authentication.getName()))
        );
    }

    @Operation(summary = "Statistiques du tableau de bord recruteur")
    @GetMapping("/recruteur")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<DashboardRecruteurResponse>> getDashboardRecruteur(
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Dashboard récupéré",
                dashboardService.getDashboardRecruteur(authentication.getName()))
        );
    }
}
