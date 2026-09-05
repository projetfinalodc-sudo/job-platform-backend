package com.odc.plateforme_emploi.service;

import com.odc.plateforme_emploi.dto.*;
import com.odc.plateforme_emploi.entity.*;
import com.odc.plateforme_emploi.exception.*;
import com.odc.plateforme_emploi.mail.EmailService;
import com.odc.plateforme_emploi.repository.*;
import com.odc.plateforme_emploi.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final CandidatRepository candidatRepository;
    private final RecruteurRepository recruteurRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    @Value("${app.activation.token.expiration-hours}")
    private long tokenExpirationHours;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Value("${app.password-reset.token.expiration-minutes}")
    private long resetTokenExpirationMinutes;

    /**
     * Inscription publique. Le compte est créé mais désactivé (enabled = false)
     * tant que l'utilisateur n'a pas cliqué sur le lien reçu par e-mail.
     *
     * ⚠️ Seuls CANDIDAT et RECRUTEUR peuvent s'inscrire ici. La création d'un
     * compte ADMIN se fait uniquement via /api/admin/utilisateurs par un
     * administrateur déjà authentifié (voir UtilisateurService.creerUtilisateur) —
     * l'ancienne version de cette méthode permettait de créer un ADMIN via cette
     * route publique, ce qui était une faille de sécurité critique.
     */
    public RegisterResponse register(RegisterRequest request) {

        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Cet email est déjà utilisé !");
        }

        String motDePasseEncode = passwordEncoder.encode(request.getMotDePasse());
        Utilisateur utilisateurCree;

        if (request.getRole() == Utilisateur.Role.CANDIDAT) {
            Candidat candidat = new Candidat();
            candidat.setNom(request.getNom());
            candidat.setPrenom(request.getPrenom());
            candidat.setEmail(request.getEmail());
            candidat.setMotDePasse(motDePasseEncode);
            candidat.setRole(Utilisateur.Role.CANDIDAT);
            candidat.setActif(true);
            candidat.setEnabled(false);
            candidat.setTelephone(request.getTelephone());
            candidat.setCompetences(request.getCompetences());
            candidat.setBiographie(request.getBiographie());
            utilisateurCree = candidatRepository.save(candidat);

        } else if (request.getRole() == Utilisateur.Role.RECRUTEUR) {
            Recruteur recruteur = new Recruteur();
            recruteur.setNom(request.getNom());
            recruteur.setPrenom(request.getPrenom());
            recruteur.setEmail(request.getEmail());
            recruteur.setMotDePasse(motDePasseEncode);
            recruteur.setRole(Utilisateur.Role.RECRUTEUR);
            recruteur.setActif(true);
            recruteur.setEnabled(false);
            recruteur.setEntreprise(request.getEntreprise() != null
                ? request.getEntreprise() : "Non renseigné");
            recruteur.setSecteur(request.getSecteur());
            recruteur.setTelephone(request.getTelephone());
            recruteur.setSiteWeb(request.getSiteWeb());
            utilisateurCree = recruteurRepository.save(recruteur);

        } else {
            throw new BadRequestException(
                "Rôle invalide ! Seuls CANDIDAT et RECRUTEUR peuvent s'inscrire directement."
            );
        }

        String token = genererEtSauvegarderToken(utilisateurCree);

        boolean emailEnvoye = true;
        try {
            emailService.envoyerEmailActivation(
                utilisateurCree.getEmail(), utilisateurCree.getPrenom(), token
            );
        } catch (Exception e) {
            emailEnvoye = false;
            log.error(
                "Compte {} créé mais l'envoi de l'e-mail d'activation a échoué. "
                + "L'utilisateur devra utiliser 'renvoyer le lien' depuis la page de connexion.",
                utilisateurCree.getEmail(), e
            );
        }

        log.info("Nouveau compte créé (en attente d'activation) : {}", utilisateurCree.getEmail());

        String message = emailEnvoye
            ? "Compte créé ! Un e-mail d'activation a été envoyé à " + utilisateurCree.getEmail() + "."
            : "Compte créé ! Nous n'avons pas pu envoyer l'e-mail d'activation pour le moment — "
              + "utilisez \"renvoyer le lien\" depuis la page de connexion.";

        return new RegisterResponse(utilisateurCree.getId(), utilisateurCree.getEmail(), message);
    }

    public AuthResponse login(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect."));

        if (!utilisateur.isEnabled()) {
            throw new AccountNotActivatedException(
                "Votre compte n'est pas encore activé. Vérifiez votre boîte e-mail (et vos spams)."
            );
        }

        if (!utilisateur.isActif()) {
            throw new org.springframework.security.authentication.DisabledException(
                "Ce compte a été suspendu par l'administrateur."
            );
        }

        // Vérifie le mot de passe. Lève BadCredentialsException si incorrect
        // (interceptée par le GlobalExceptionHandler → 401).
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getMotDePasse()
            )
        );

        String token = jwtUtils.generateToken(utilisateur.getEmail());
        String refreshToken = genererEtSauvegarderRefreshToken(utilisateur);

        return new AuthResponse(token, refreshToken, utilisateur.getId(), utilisateur.getNom(),
                utilisateur.getPrenom(), utilisateur.getEmail(), utilisateur.getRole());
    }

    /**
     * Échange un refresh token valide contre un nouvel access token.
     * Le refresh token est "roté" (l'ancien est révoqué, un nouveau est émis) à
     * chaque utilisation : si un refresh token volé est utilisé après le
     * légitime, le prochain refresh du légitime échouera et l'alertera.
     */
    @Transactional
    public AuthResponse rafraichirToken(String refreshTokenValue) {
        RefreshToken ancien = refreshTokenRepository.findByToken(refreshTokenValue)
            .orElseThrow(() -> new InvalidTokenException("Refresh token invalide."));

        if (!ancien.estValide()) {
            throw new ExpiredTokenException("Votre session a expiré, merci de vous reconnecter.");
        }

        Utilisateur utilisateur = ancien.getUtilisateur();

        if (!utilisateur.isEnabled() || !utilisateur.isActif()) {
            throw new org.springframework.security.authentication.DisabledException(
                "Ce compte n'est plus actif."
            );
        }

        ancien.setRevoque(true);
        refreshTokenRepository.save(ancien);

        String nouvelAccessToken = jwtUtils.generateToken(utilisateur.getEmail());
        String nouveauRefreshToken = genererEtSauvegarderRefreshToken(utilisateur);

        return new AuthResponse(nouvelAccessToken, nouveauRefreshToken, utilisateur.getId(),
            utilisateur.getNom(), utilisateur.getPrenom(), utilisateur.getEmail(), utilisateur.getRole());
    }

    // Déconnexion : révoque le refresh token pour qu'il ne puisse plus être réutilisé
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
            .ifPresent(rt -> {
                rt.setRevoque(true);
                refreshTokenRepository.save(rt);
            });
    }

    /**
     * Démarre une réinitialisation de mot de passe. Ne révèle jamais si l'e-mail
     * existe ou non (message générique dans tous les cas) pour éviter qu'un tiers
     * puisse énumérer les comptes existants via ce endpoint.
     */
    public void demanderReinitialisationMotDePasse(String email) {
        utilisateurRepository.findByEmail(email).ifPresent(utilisateur -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUtilisateur(utilisateur);
            resetToken.setDateExpiration(LocalDateTime.now().plusMinutes(resetTokenExpirationMinutes));
            passwordResetTokenRepository.save(resetToken);

            try {
                emailService.envoyerEmailReinitialisationMotDePasse(
                    utilisateur.getEmail(), utilisateur.getPrenom(), token
                );
            } catch (Exception e) {
                log.error("Échec de l'envoi de l'e-mail de réinitialisation pour {}", email, e);
            }
        });

        log.info("Demande de réinitialisation de mot de passe pour : {}", email);
    }

    @Transactional
    public void reinitialiserMotDePasse(String token, String nouveauMotDePasse) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException(
                "Ce lien de réinitialisation est invalide ou a déjà été utilisé."
            ));

        if (resetToken.estExpire()) {
            throw new ExpiredTokenException(
                "Ce lien de réinitialisation a expiré. Merci de refaire une demande."
            );
        }

        Utilisateur utilisateur = resetToken.getUtilisateur();
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);
        passwordResetTokenRepository.delete(resetToken);

        log.info("Mot de passe réinitialisé pour : {}", utilisateur.getEmail());
    }

    // Changement de mot de passe par un utilisateur déjà connecté
    public void changerMotDePasse(String emailUtilisateur, String ancienMotDePasse, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé !"));

        if (!passwordEncoder.matches(ancienMotDePasse, utilisateur.getMotDePasse())) {
            throw new BadRequestException("L'ancien mot de passe est incorrect.");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);

        log.info("Mot de passe changé par l'utilisateur : {}", emailUtilisateur);
    }

    private String genererEtSauvegarderRefreshToken(Utilisateur utilisateur) {
        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUtilisateur(utilisateur);
        refreshToken.setDateCreation(LocalDateTime.now());
        refreshToken.setDateExpiration(LocalDateTime.now().plusDays(refreshExpirationDays));

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    /**
     * Active le compte associé au token si celui-ci est valide et non expiré.
     * Le token est supprimé après usage (à usage unique).
     */
    @Transactional
    public void activerCompte(String token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException(
                "Ce lien d'activation est invalide ou a déjà été utilisé."
            ));

        if (activationToken.estExpire()) {
            throw new ExpiredTokenException(
                "Ce lien d'activation a expiré. Merci de demander un nouveau lien."
            );
        }

        Utilisateur utilisateur = activationToken.getUtilisateur();
        utilisateur.setEnabled(true);
        utilisateurRepository.save(utilisateur);
        activationTokenRepository.delete(activationToken);

        log.info("Compte activé : {}", utilisateur.getEmail());
    }

    /**
     * Génère et envoie un nouveau lien d'activation. L'ancien lien reste valide
     * jusqu'à son expiration naturelle (pas de suppression pour éviter tout
     * problème de concurrence si l'utilisateur a plusieurs onglets ouverts).
     */
    public void renvoyerActivation(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Aucun compte n'est associé à cet e-mail."
            ));

        if (utilisateur.isEnabled()) {
            throw new BadRequestException("Ce compte est déjà activé. Vous pouvez vous connecter.");
        }

        String token = genererEtSauvegarderToken(utilisateur);
        emailService.envoyerEmailActivation(utilisateur.getEmail(), utilisateur.getPrenom(), token);

        log.info("Nouveau lien d'activation envoyé : {}", utilisateur.getEmail());
    }

    private String genererEtSauvegarderToken(Utilisateur utilisateur) {
        String token = UUID.randomUUID().toString();

        ActivationToken activationToken = new ActivationToken();
        activationToken.setToken(token);
        activationToken.setUtilisateur(utilisateur);
        activationToken.setDateCreation(LocalDateTime.now());
        activationToken.setDateExpiration(LocalDateTime.now().plusHours(tokenExpirationHours));

        activationTokenRepository.save(activationToken);
        return token;
    }
}
