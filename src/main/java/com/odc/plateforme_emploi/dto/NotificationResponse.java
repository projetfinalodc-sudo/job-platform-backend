package com.odc.plateforme_emploi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String titre;
    private String message;
    private String lien;
    private String type;
    private boolean lu;
    private LocalDateTime dateCreation;
}
