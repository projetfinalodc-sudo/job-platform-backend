package com.odc.plateforme_emploi.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardRecruteurResponse {
    private long offresPubliees;
    private long offresActives;
    private long candidaturesRecues;
    private long candidatsUniques;
    private List<CandidatureResponse> dernieresCandidatures;
    private List<OffreResponse> dernieresOffres;
}
