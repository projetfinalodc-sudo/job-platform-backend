package com.odc.plateforme_emploi.repository;



import com.odc.plateforme_emploi.entity.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    // Toutes les candidatures d'un candidat
    List<Candidature> findByCandidatId(Long candidatId);

    // Toutes les candidatures pour une offre
    List<Candidature> findByOffreId(Long offreId);

    // Vérifier si un candidat a déjà postulé à une offre
    boolean existsByCandidatIdAndOffreId(Long candidatId, Long offreId);

    // Candidatures par statut
    List<Candidature> findByStatut(Candidature.StatutCandidature statut);

    // Compter les candidatures d'un candidat par statut (dashboard candidat)
    long countByCandidatIdAndStatut(Long candidatId, Candidature.StatutCandidature statut);

    // Les N dernières candidatures d'un candidat, les plus récentes en premier
    List<Candidature> findTop5ByCandidatIdOrderByDateCandidatureDesc(Long candidatId);

    // Toutes les candidatures reçues sur les offres d'un recruteur (dashboard recruteur)
    List<Candidature> findByOffreRecruteurId(Long recruteurId);

    // Les N dernières candidatures reçues sur les offres d'un recruteur
    List<Candidature> findTop5ByOffreRecruteurIdOrderByDateCandidatureDesc(Long recruteurId);

    // Nombre de candidats distincts ayant postulé à au moins une offre d'un recruteur
    @Query("SELECT COUNT(DISTINCT c.candidat.id) FROM Candidature c WHERE c.offre.recruteur.id = :recruteurId")
    long countDistinctCandidatByOffreRecruteurId(@Param("recruteurId") Long recruteurId);
}
