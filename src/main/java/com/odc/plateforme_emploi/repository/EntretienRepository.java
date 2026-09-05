package com.odc.plateforme_emploi.repository;

import com.odc.plateforme_emploi.entity.Entretien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntretienRepository extends JpaRepository<Entretien, Long> {
    Optional<Entretien> findByCandidatureId(Long candidatureId);
    java.util.List<Entretien> findByCandidature_Candidat_EmailOrderByDateHeureAsc(String email);
}
