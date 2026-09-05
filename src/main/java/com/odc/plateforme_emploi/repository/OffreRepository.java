package com.odc.plateforme_emploi.repository;

import com.odc.plateforme_emploi.entity.Offre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OffreRepository extends JpaRepository<Offre, Long> {

    // Toutes les offres d'un recruteur
    List<Offre> findByRecruteurId(Long recruteurId);

    // Offres actives uniquement
    List<Offre> findByStatut(Offre.StatutOffre statut);
    long countByStatut(Offre.StatutOffre statut);

    // Recherche par titre ou localisation
    List<Offre> findByTitreContainingIgnoreCaseOrLocalisationContainingIgnoreCase(
        String titre, String localisation
    );

    // Offres par type de contrat
    List<Offre> findByTypeContrat(String typeContrat);

    // Compter les offres d'un recruteur (dashboard recruteur)
    long countByRecruteurId(Long recruteurId);
    long countByRecruteurIdAndStatut(Long recruteurId, Offre.StatutOffre statut);

    // Les N dernières offres d'un recruteur, les plus récentes en premier
    List<Offre> findTop5ByRecruteurIdOrderByDatePublicationDesc(Long recruteurId);
}
