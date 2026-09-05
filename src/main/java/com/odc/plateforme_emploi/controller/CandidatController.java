package com.odc.plateforme_emploi.controller;

import com.odc.plateforme_emploi.dto.ApiResponse;
import com.odc.plateforme_emploi.entity.Candidat;
import com.odc.plateforme_emploi.exception.ResourceNotFoundException;
import com.odc.plateforme_emploi.exception.UnauthorizedOperationException;
import com.odc.plateforme_emploi.repository.CandidatRepository;
import com.odc.plateforme_emploi.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDAT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "CV Candidat", description = "Upload et téléchargement du CV de profil (candidat)")
public class CandidatController {

    private final CandidatRepository candidatRepository;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Uploader / remplacer son CV de profil", description = "PDF/DOC/DOCX, 5 Mo max. Utilisé pour pré-remplir les candidatures.")
    @PostMapping("/cv")
    public ResponseEntity<ApiResponse<String>> uploadCV(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Candidat candidat = candidatRepository
            .findByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé !"));

        String ancienCv = candidat.getCvPath();

        String fileName = fileStorageService.sauvegarderCV(file);
        candidat.setCvPath(fileName);
        candidatRepository.save(candidat);

        // On supprime l'ancien fichier seulement après le succès de l'enregistrement
        // du nouveau, pour ne jamais se retrouver sans CV en cas d'erreur intermédiaire.
        if (ancienCv != null) {
            fileStorageService.supprimerCV(ancienCv);
        }

        return ResponseEntity.ok(
            ApiResponse.success("CV uploadé avec succès !", fileName)
        );
    }

    // Télécharger son propre CV de profil (aucun autre utilisateur ne peut y accéder ainsi ;
    // pour télécharger le CV joint à une candidature précise, voir CandidatureController
    // qui vérifie la propriété candidat/recruteur au niveau de la candidature).
    @Operation(summary = "Télécharger son propre CV de profil")
    @GetMapping("/cv/{fileName:.+}")
    public ResponseEntity<Resource> downloadCV(
            @PathVariable String fileName, Authentication authentication) {

        Candidat candidat = candidatRepository
            .findByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé !"));

        if (!fileName.equals(candidat.getCvPath())) {
            throw new UnauthorizedOperationException("Vous ne pouvez télécharger que votre propre CV.");
        }

        Resource resource = fileStorageService.chargerCV(fileName);
        return construireReponseTelechargement(resource);
    }

    private ResponseEntity<Resource> construireReponseTelechargement(Resource resource) {
        String contentType = "application/octet-stream";
        String nom = resource.getFilename();
        if (nom != null && nom.toLowerCase().endsWith(".pdf")) {
            contentType = "application/pdf";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + nom + "\"")
            .body(resource);
    }
}
