package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.ApiResponse;
import com.odc.plateforme_emploi.dto.NotificationResponse;
import com.odc.plateforme_emploi.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "Notifications internes de l'utilisateur connecté")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Lister mes notifications")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMesNotifications(
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Notifications récupérées",
                notificationService.getMesNotifications(authentication.getName()))
        );
    }

    @Operation(summary = "Compter mes notifications non lues")
    @GetMapping("/non-lues/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> compterNonLues(Authentication authentication) {
        long count = notificationService.compterNonLues(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Compte récupéré", Map.of("count", count)));
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PutMapping("/{id}/lue")
    public ResponseEntity<ApiResponse<NotificationResponse>> marquerCommeLue(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Notification marquée comme lue",
                notificationService.marquerCommeLue(id, authentication.getName()))
        );
    }

    @Operation(summary = "Marquer toutes mes notifications comme lues")
    @PutMapping("/marquer-toutes-lues")
    public ResponseEntity<ApiResponse<Void>> marquerToutesCommeLues(Authentication authentication) {
        notificationService.marquerToutesCommeLues(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Toutes les notifications ont été marquées comme lues"));
    }

    @Operation(summary = "Supprimer une notification")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(@PathVariable Long id, Authentication authentication) {
        notificationService.supprimer(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Notification supprimée"));
    }
}
