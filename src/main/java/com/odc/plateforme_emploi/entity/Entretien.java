package com.odc.plateforme_emploi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entretien proposé par le recruteur pour une candidature ACCEPTEE, comme
 * dans un vrai processus de recrutement : après l'acceptation, le recruteur
 * planifie un rendez-vous (présentiel, visio ou téléphone) que le candidat
 * peut ensuite confirmer ou décliner.
 *
 * Une candidature n'a qu'un seul entretien actif à la fois : reproposer un
 * entretien (autre date) met à jour celui-ci plutôt que d'en créer un nouveau,
 * ce qui garde l'historique simple pour ce projet.
 */
@Entity
@Table(name = "entretiens")
@Data
@NoArgsConstructor
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false, unique = true)
    private Candidature candidature;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalite modalite;

    // Adresse physique si PRESENTIEL, lien de visio si VISIO, numéro si TELEPHONE
    @Column(name = "lieu_ou_lien")
    private String lieuOuLien;

    // Message libre du recruteur au candidat (consignes, contact sur place...)
    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEntretien statut = StatutEntretien.PROPOSE;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    public enum Modalite {
        PRESENTIEL, VISIO, TELEPHONE
    }

    public enum StatutEntretien {
        PROPOSE, CONFIRME, REFUSE, ANNULE
    }
}
