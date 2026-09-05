package com.odc.plateforme_emploi.service;

import com.odc.plateforme_emploi.dto.DashboardCandidatResponse;
import com.odc.plateforme_emploi.dto.DashboardRecruteurResponse;
import com.odc.plateforme_emploi.entity.Candidat;
import com.odc.plateforme_emploi.entity.Candidature;
import com.odc.plateforme_emploi.entity.Offre;
import com.odc.plateforme_emploi.entity.Recruteur;
import com.odc.plateforme_emploi.exception.ResourceNotFoundException;
import com.odc.plateforme_emploi.repository.CandidatRepository;
import com.odc.plateforme_emploi.repository.CandidatureRepository;
import com.odc.plateforme_emploi.repository.OffreRepository;
import com.odc.plateforme_emploi.repository.RecruteurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CandidatRepository candidatRepository;
    private final RecruteurRepository recruteurRepository;
    private final CandidatureRepository candidatureRepository;
    private final OffreRepository offreRepository;
    private final CandidatureService candidatureService;
    private final OffreService offreService;
    private final EntretienService entretienService;

    public DashboardCandidatResponse getDashboardCandidat(String emailCandidat) {
        Candidat candidat = candidatRepository.findByEmail(emailCandidat)
            .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé !"));

        DashboardCandidatResponse dashboard = new DashboardCandidatResponse();
        dashboard.setCandidaturesEnvoyees(
            candidatureRepository.findByCandidatId(candidat.getId()).size());
        dashboard.setCandidaturesEnAttente(
            candidatureRepository.countByCandidatIdAndStatut(candidat.getId(), Candidature.StatutCandidature.EN_ATTENTE)
            + candidatureRepository.countByCandidatIdAndStatut(candidat.getId(), Candidature.StatutCandidature.VUE));
        dashboard.setCandidaturesAcceptees(
            candidatureRepository.countByCandidatIdAndStatut(candidat.getId(), Candidature.StatutCandidature.ACCEPTEE));
        dashboard.setCandidaturesRefusees(
            candidatureRepository.countByCandidatIdAndStatut(candidat.getId(), Candidature.StatutCandidature.REFUSEE));
        dashboard.setOffresDisponibles(
            offreRepository.findByStatut(Offre.StatutOffre.ACTIVE).size());
        dashboard.setDernieresCandidatures(
            candidatureService.toResponses(
                candidatureRepository.findTop5ByCandidatIdOrderByDateCandidatureDesc(candidat.getId())
            ));
        dashboard.setProchainsEntretiens(
            entretienService.getMesEntretiens(emailCandidat).stream()
                .filter(e -> e.getStatut() != com.odc.plateforme_emploi.entity.Entretien.StatutEntretien.ANNULE
                    && e.getStatut() != com.odc.plateforme_emploi.entity.Entretien.StatutEntretien.REFUSE
                    && e.getDateHeure().isAfter(java.time.LocalDateTime.now()))
                .toList()
        );

        return dashboard;
    }

    public DashboardRecruteurResponse getDashboardRecruteur(String emailRecruteur) {
        Recruteur recruteur = recruteurRepository.findByEmail(emailRecruteur)
            .orElseThrow(() -> new ResourceNotFoundException("Recruteur non trouvé !"));

        DashboardRecruteurResponse dashboard = new DashboardRecruteurResponse();
        dashboard.setOffresPubliees(offreRepository.countByRecruteurId(recruteur.getId()));
        dashboard.setOffresActives(
            offreRepository.countByRecruteurIdAndStatut(recruteur.getId(), Offre.StatutOffre.ACTIVE));
        dashboard.setCandidaturesRecues(
            candidatureRepository.findByOffreRecruteurId(recruteur.getId()).size());
        dashboard.setCandidatsUniques(
            candidatureRepository.countDistinctCandidatByOffreRecruteurId(recruteur.getId()));
        dashboard.setDernieresCandidatures(
            candidatureService.toResponses(
                candidatureRepository.findTop5ByOffreRecruteurIdOrderByDateCandidatureDesc(recruteur.getId())
            ));
        dashboard.setDernieresOffres(
            offreService.toResponses(
                offreRepository.findTop5ByRecruteurIdOrderByDatePublicationDesc(recruteur.getId())
            ));

        return dashboard;
    }
}
