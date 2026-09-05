package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.*;
import com.odc.plateforme_emploi.entity.Candidature;
import com.odc.plateforme_emploi.service.CandidatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Candidatures", description = "Postuler, consulter et gérer les candidatures")
public class CandidatureController {

    private final CandidatureService candidatureService;

    @Operation(summary = "Postuler à une offre", description = "Multipart : CV obligatoire (PDF/DOC/DOCX, 5 Mo max), lettre de motivation optionnelle (fichier OU texte).")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> postuler(
            @RequestParam Long offreId,
            @RequestParam("cv") MultipartFile cv,
            @RequestParam(value = "lettreMotivationFichier", required = false) MultipartFile lettreMotivationFichier,
            @RequestParam(value = "lettreMotivationTexte", required = false) String lettreMotivationTexte,
            @RequestParam String telephoneContact,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDisponibilite,
            Authentication authentication) {
        CandidatureResponse response = candidatureService.postuler(
            offreId, cv, lettreMotivationFichier, lettreMotivationTexte,
            telephoneContact, dateDisponibilite, authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Candidature envoyée !", response));
    }

    @Operation(summary = "Lister mes candidatures (candidat)")
    @GetMapping("/mes-candidatures")
    public ResponseEntity<ApiResponse<List<CandidatureResponse>>> getMesCandidatures(
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Mes candidatures",
                candidatureService.getMesCandidatures(authentication.getName()))
        );
    }

    @Operation(summary = "Candidatures reçues pour une offre", description = "Doit être le recruteur propriétaire de l'offre.")
    @GetMapping("/offre/{offreId}")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<List<CandidatureResponse>>> getCandidaturesOffre(
            @PathVariable Long offreId,
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Candidatures reçues",
                candidatureService.getCandidaturesOffre(offreId, authentication.getName()))
        );
    }

    @Operation(summary = "Changer le statut d'une candidature", description = "ACCEPTEE/REFUSEE déclenchent une notification (email + interne) au candidat.")
    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('RECRUTEUR')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> changerStatut(
            @PathVariable Long id,
            @RequestParam Candidature.StatutCandidature statut,
            Authentication authentication) {
        return ResponseEntity.ok(
            ApiResponse.success("Statut mis à jour",
                candidatureService.changerStatut(id, statut, authentication.getName()))
        );
    }

    @Operation(summary = "Télécharger le CV d'une candidature", description = "Accessible au candidat propriétaire ou au recruteur propriétaire de l'offre.")
    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> telechargerCvCandidature(
            @PathVariable Long id, Authentication authentication) {
        Resource resource = candidatureService.chargerCvCandidature(id, authentication.getName());
        return construireReponseTelechargement(resource);
    }

    @Operation(summary = "Télécharger la lettre de motivation d'une candidature")
    @GetMapping("/{id}/lettre")
    public ResponseEntity<Resource> telechargerLettreCandidature(
            @PathVariable Long id, Authentication authentication) {
        Resource resource = candidatureService.chargerLettreCandidature(id, authentication.getName());
        return construireReponseTelechargement(resource);
    }

    private ResponseEntity<Resource> construireReponseTelechargement(Resource resource) {
        String nom = resource.getFilename();
        String contentType = (nom != null && nom.toLowerCase().endsWith(".pdf"))
            ? "application/pdf" : "application/octet-stream";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
            .body(resource);
    }
}