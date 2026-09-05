package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.*;
import com.odc.plateforme_emploi.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profil", description = "Consultation et modification de son propre profil")
public class ProfilController {

    private final UtilisateurService utilisateurService;

    @Operation(summary = "Voir mon profil")
    @GetMapping
    public ResponseEntity<ApiResponse<UtilisateurResponse>> monProfil(
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Profil récupéré",
                utilisateurService.getMonProfil(authentication.getName()))
        );
    }

    @Operation(summary = "Modifier mon profil", description = "Le mot de passe ne se change pas ici, voir /api/auth/change-password.")
    @PutMapping
    public ResponseEntity<ApiResponse<UtilisateurResponse>> modifierMonProfil(
            @Valid @RequestBody UpdateProfilRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Profil mis à jour !",
                utilisateurService.modifierMonProfil(authentication.getName(), request))
        );
    }
}
