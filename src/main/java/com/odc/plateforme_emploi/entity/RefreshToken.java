package com.odc.plateforme_emploi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    // Un refresh token révoqué (déconnexion, ou déjà échangé contre un nouveau) ne
    // peut plus être utilisé même s'il n'a pas encore expiré.
    @Column(nullable = false)
    private boolean revoque = false;

    public boolean estValide() {
        return !revoque && dateExpiration.isAfter(LocalDateTime.now());
    }
}
