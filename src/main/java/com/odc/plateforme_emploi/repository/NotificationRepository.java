package com.odc.plateforme_emploi.repository;

import com.odc.plateforme_emploi.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);
    long countByUtilisateurIdAndLuFalse(Long utilisateurId);
    List<Notification> findByUtilisateurIdAndLuFalse(Long utilisateurId);
}
