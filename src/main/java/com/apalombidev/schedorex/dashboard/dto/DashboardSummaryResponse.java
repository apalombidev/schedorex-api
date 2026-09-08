package com.apalombidev.schedorex.dashboard.dto;

import java.time.Instant;

/**
 * Riepilogo per il banner dashboard admin (v2 — "meno di 7 giorni di slot
 * futuri generati"). latestSlotStart è null se il servizio non ha ancora
 * nessuno slot generato (caso "calendario vuoto", ancora più urgente del
 * semplice warning).
 */
public record DashboardSummaryResponse(
        boolean lowAvailabilityWarning,
        Instant latestSlotStart,
        Instant checkedAt
) {
}