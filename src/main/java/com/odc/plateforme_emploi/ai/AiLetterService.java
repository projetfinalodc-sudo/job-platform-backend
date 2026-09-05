package com.odc.plateforme_emploi.ai;

import com.odc.plateforme_emploi.entity.Candidat;
import com.odc.plateforme_emploi.entity.Offre;
import com.odc.plateforme_emploi.exception.ResourceNotFoundException;
import com.odc.plateforme_emploi.repository.CandidatRepository;
import com.odc.plateforme_emploi.repository.OffreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiLetterService {

    private final CandidatRepository candidatRepository;
    private final OffreRepository offreRepository;
    private final AiLetterGenerator aiLetterGenerator;

    /**
     * Génère un brouillon de lettre de motivation pour un candidat et une offre
     * donnés. Le candidat reste libre de modifier le texte avant de postuler —
     * ceci ne fait que pré-remplir le champ côté frontend.
     */
    public String genererLettrePourOffre(Long offreId, String emailCandidat) {
        Candidat candidat = candidatRepository.findByEmail(emailCandidat)
            .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé !"));

        Offre offre = offreRepository.findById(offreId)
            .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée !"));

        LetterGenerationContext contexte = new LetterGenerationContext(
            candidat.getPrenom(),
            candidat.getNom(),
            candidat.getCompetences(),
            candidat.getBiographie(),
            offre.getTitre(),
            offre.getRecruteur().getEntreprise(),
            offre.getDescription()
        );

        log.info("Génération IA d'une lettre de motivation : candidat={}, offre={}", emailCandidat, offreId);
        return aiLetterGenerator.genererLettreMotivation(contexte);
    }
}
