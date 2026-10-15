package com.apalombidev.schedorex.dashboard.dto;

import java.time.Instant;

public record DashboardSummaryResponse(boolean lowAvailabilityWarning, Instant latestSlotStart, Instant checkedAt) {
}