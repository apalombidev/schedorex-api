package com.apalombidev.schedorex.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AvailabilityGenerationRequest(

		@NotNull LocalDate startDate,

		@NotNull LocalDate endDate,

		@NotEmpty Set<DayOfWeek> activeDays,

		@NotNull LocalTime openingTime,

		@NotNull LocalTime closingTime,

		LocalTime lunchBreakStart,

		LocalTime lunchBreakEnd) {

}
