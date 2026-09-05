package com.odc.plateforme_emploi.dto;

import com.odc.plateforme_emploi.validation.Telephone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfilRequest {
    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @Email
    @NotBlank
    private String email;

    // Candidat
    @Telephone
    private String telephone;
    private String competences;
    private String biographie;

    // Recruteur
    private String entreprise;
    private String secteur;
    private String siteWeb;
}