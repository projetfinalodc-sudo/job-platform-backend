package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.*;
import com.odc.plateforme_emploi.service.UtilisateurService;
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
@RequestMapping("/api/admin/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administration", description = "Gestion des utilisateurs — réservé au rôle ADMIN")
public class AdminController {

    private final UtilisateurService utilisateurService;

    @Operation(summary = "Lister tous les utilisateurs")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UtilisateurResponse>>> getTous() {
        return ResponseEntity.ok(
            ApiResponse.success("Utilisateurs récupérés",
                utilisateurService.getTousLesUtilisateurs())
        );
    }

    @Operation(summary = "Détail d'un utilisateur")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.success("Utilisateur trouvé",
                utilisateurService.getUtilisateurById(id))
        );
    }

    @Operation(summary = "Créer un utilisateur (candidat, recruteur ou admin)")
    @PostMapping
    public ResponseEntity<ApiResponse<UtilisateurResponse>> creer(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success("Utilisateur créé !",
                utilisateurService.creerUtilisateur(request))
        );
    }

    @Operation(summary = "Modifier un utilisateur")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success("Utilisateur modifié !",
                utilisateurService.adminModifierUtilisateur(id, request))
        );
    }

    @Operation(summary = "Suspendre ou réactiver un utilisateur", description = "Un admin ne peut pas se suspendre lui-même.")
    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> changerStatut(
            @PathVariable Long id,
            @RequestParam boolean actif,
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success(actif ? "Utilisateur réactivé !" : "Utilisateur suspendu !",
                utilisateurService.changerStatutActif(id, actif, authentication.getName()))
        );
    }

    @Operation(summary = "Supprimer un utilisateur", description = "Un admin ne peut pas se supprimer lui-même.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @PathVariable Long id,
            Authentication authentication) {
        utilisateurService.supprimerUtilisateur(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Utilisateur supprimé !"));
    }
}