package com.odc.plateforme_emploi.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardCandidatResponse {
    private long candidaturesEnvoyees;
    private long candidaturesEnAttente;
    private long candidaturesAcceptees;
    private long candidaturesRefusees;
    private long offresDisponibles;
    private List<CandidatureResponse> dernieresCandidatures;
    private List<EntretienResponse> prochainsEntretiens;
}
