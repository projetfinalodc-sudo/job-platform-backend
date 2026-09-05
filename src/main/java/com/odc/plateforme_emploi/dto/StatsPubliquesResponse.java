package com.odc.plateforme_emploi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsPubliquesResponse {
    private long offresActives;
    private long candidatsInscrits;
    private long recruteursInscrits;
}
