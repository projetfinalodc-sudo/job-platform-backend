package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.*;
import com.odc.plateforme_emploi.service.OffreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
@Tag(name = "Offres", description = "Consultation publique et gestion des offres d'emploi (recruteur)")
public class OffreController {

    private final OffreService offreService;

    @Operation(summary = "Lister toutes les offres actives (public)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OffreResponse>>> getToutesLesOffres() {
        return ResponseEntity.ok(
            ApiResponse.success("Offres récupérées",
                offreService.getToutesLesOffres())
        );
    }

    @Operation(summary = "Détail d'une offre (public)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OffreResponse>> getOffreById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.success("Offre trouvée", offreService.getOffreById(id))
        );
    }

    @Operation(summary = "Rechercher des offres par titre ou localisation (public)")
    @GetMapping("/recherche")
    public ResponseEntity<ApiResponse<List<OffreResponse>>> rechercherOffres(
            @RequestParam String keyword) {
        return ResponseEntity.ok(
            ApiResponse.success("Résultats",
                offreService.rechercherOffres(keyword))
        );
    }

    @Operation(summary = "Publier une offre", description = "Notifie automatiquement (email + notification interne) tous les candidats actifs.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<OffreResponse>> creerOffre(
            @Valid @RequestBody OffreRequest request,
            Authentication authentication) {
        OffreResponse response = offreService.creerOffre(
            request, authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Offre créée !", response));
    }

    @Operation(summary = "Lister mes offres")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mes-offres")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<List<OffreResponse>>> getMesOffres(
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Mes offres",
                offreService.getOffresRecruteur(authentication.getName()))
        );
    }

    @Operation(summary = "Modifier une offre", description = "Doit être le propriétaire de l'offre.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<OffreResponse>> modifierOffre(
            @PathVariable Long id,
            @Valid @RequestBody OffreRequest request,
            Authentication authentication) {
        OffreResponse response = offreService.modifierOffre(
            id, request, authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Offre modifiée !", response));
    }

    @Operation(summary = "Supprimer une offre", description = "Doit être le propriétaire de l'offre.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<Void>> supprimerOffre(
            @PathVariable Long id,
            Authentication authentication) {
        offreService.supprimerOffre(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Offre supprimée !"));
    }
}
