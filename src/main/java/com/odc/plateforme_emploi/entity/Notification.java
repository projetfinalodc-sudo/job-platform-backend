package com.odc.plateforme_emploi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, length = 500)
    private String message;

    // Lien relatif vers lequel le frontend redirige au clic (ex: "/offres/12"),
    // nullable si la notification n'a pas de destination précise.
    private String lien;

    // Catégorie utilisée côté frontend pour choisir l'icône (NOUVELLE_OFFRE,
    // NOUVELLE_CANDIDATURE, CANDIDATURE_ACCEPTEE, CANDIDATURE_REFUSEE...).
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean lu = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
