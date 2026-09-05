package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.ApiResponse;
import com.odc.plateforme_emploi.dto.EntretienResponse;
import com.odc.plateforme_emploi.dto.ProposerEntretienRequest;
import com.odc.plateforme_emploi.service.EntretienService;
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
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Entretiens", description = "Planification et réponse aux entretiens d'embauche")
public class EntretienController {

    private final EntretienService entretienService;

    @Operation(summary = "Proposer (ou reprogrammer) un entretien",
        description = "Réservé au recruteur propriétaire de l'offre, uniquement pour une candidature ACCEPTEE.")
    @PostMapping("/api/candidatures/{candidatureId}/entretien")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<EntretienResponse>> proposer(
            @PathVariable Long candidatureId,
            @Valid @RequestBody ProposerEntretienRequest request,
            Authentication authentication) {
        EntretienResponse response = entretienService.proposerEntretien(
            candidatureId, request, authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Entretien proposé au candidat !", response));
    }

    @Operation(summary = "Consulter l'entretien d'une candidature",
        description = "Accessible au candidat propriétaire ou au recruteur propriétaire de l'offre.")
    @GetMapping("/api/candidatures/{candidatureId}/entretien")
    public ResponseEntity<ApiResponse<EntretienResponse>> consulter(
            @PathVariable Long candidatureId,
            Authentication authentication) {
        EntretienResponse response = entretienService.getEntretien(candidatureId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Entretien trouvé", response));
    }

    @Operation(summary = "Confirmer ou décliner l'entretien proposé", description = "Réservé au candidat.")
    @PutMapping("/api/candidatures/{candidatureId}/entretien/reponse")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApiResponse<EntretienResponse>> repondre(
            @PathVariable Long candidatureId,
            @RequestParam boolean accepte,
            Authentication authentication) {
        EntretienResponse response = entretienService.repondreEntretien(
            candidatureId, accepte, authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success(
            accepte ? "Entretien confirmé !" : "Entretien décliné.", response
        ));
    }

    @Operation(summary = "Annuler un entretien", description = "Réservé au recruteur propriétaire de l'offre.")
    @DeleteMapping("/api/candidatures/{candidatureId}/entretien")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<Void>> annuler(
            @PathVariable Long candidatureId,
            Authentication authentication) {
        entretienService.annulerEntretien(candidatureId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Entretien annulé."));
    }

    @Operation(summary = "Mes entretiens à venir", description = "Réservé au candidat, triés par date croissante.")
    @GetMapping("/api/entretiens/mes-entretiens")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApiResponse<List<EntretienResponse>>> mesEntretiens(Authentication authentication) {
        List<EntretienResponse> reponse = entretienService.getMesEntretiens(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Entretiens récupérés", reponse));
    }
}
