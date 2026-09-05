package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.*;
import com.odc.plateforme_emploi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, activation, mot de passe, refresh token")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Inscription (candidat ou recruteur)", description = "Le compte est créé désactivé (enabled=false) ; un e-mail d'activation est envoyé.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Inscription réussie !", response));
    }

    @Operation(summary = "Connexion", description = "Renvoie un access token (30 min) et un refresh token (30 jours).")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie !", response));
    }

    // Échange un refresh token valide contre un nouvel access token
    // (appelé automatiquement par le frontend quand l'access token expire)
    @Operation(summary = "Renouveler l'access token", description = "Échange un refresh token valide contre un nouveau couple access/refresh token (rotation).")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.rafraichirToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token renouvelé !", response));
    }

    // Révoque le refresh token côté serveur (déconnexion réelle, pas juste côté client)
    @Operation(summary = "Déconnexion", description = "Révoque le refresh token côté serveur.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie !"));
    }

    // Appelé quand l'utilisateur clique sur le lien reçu par e-mail
    @Operation(summary = "Activer un compte", description = "Appelé via le lien reçu par e-mail.")
    @GetMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@RequestParam String token) {
        authService.activerCompte(token);
        return ResponseEntity.ok(ApiResponse.success("Compte activé avec succès !"));
    }

    // Permet de redemander un lien si l'ancien a expiré
    @Operation(summary = "Renvoyer le lien d'activation")
    @PostMapping("/resend-activation")
    public ResponseEntity<ApiResponse<Void>> resendActivation(
            @Valid @RequestBody ResendActivationRequest request) {
        authService.renvoyerActivation(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Un nouveau lien d'activation a été envoyé !"));
    }

    // Mot de passe oublié : envoie un lien de réinitialisation par e-mail
    @Operation(summary = "Mot de passe oublié", description = "Envoie un lien de réinitialisation si l'e-mail existe (réponse identique dans tous les cas, anti-énumération).")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.demanderReinitialisationMotDePasse(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
            "Si un compte existe avec cet e-mail, un lien de réinitialisation a été envoyé."
        ));
    }

    // Réinitialisation effective via le token reçu par e-mail
    @Operation(summary = "Réinitialiser le mot de passe", description = "Via le token reçu par e-mail (valable 30 min).")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.reinitialiserMotDePasse(request.getToken(), request.getNouveauMotDePasse());
        return ResponseEntity.ok(ApiResponse.success("Mot de passe réinitialisé avec succès !"));
    }

    // Changement de mot de passe par un utilisateur déjà connecté
    @Operation(summary = "Changer son mot de passe", description = "Utilisateur connecté ; l'ancien mot de passe est requis.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changerMotDePasse(
            authentication.getName(), request.getAncienMotDePasse(), request.getNouveauMotDePasse()
        );
        return ResponseEntity.ok(ApiResponse.success("Mot de passe changé avec succès !"));
    }
}