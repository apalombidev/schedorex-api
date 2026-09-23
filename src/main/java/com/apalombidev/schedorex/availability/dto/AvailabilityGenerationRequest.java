package com.apalombidev.schedorex.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AvailabilityGenerationRequest(

        @NotNull(message = "{validation.notnull}")
        LocalDate startDate,

        @NotNull(message = "{validation.notnull}")
        LocalDate endDate,

        @NotEmpty(message = "{validation.notempty}")
        Set<DayOfWeek> activeDays,

        @NotNull(message = "{validation.notnull}")
        LocalTime openingTime,

        @NotNull(message = "{validation.notnull}")
        LocalTime closingTime,

        LocalTime lunchBreakStart,

        LocalTime lunchBreakEnd
) {
}