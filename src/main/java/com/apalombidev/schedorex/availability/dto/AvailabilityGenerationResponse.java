package com.apalombidev.schedorex.availability.dto;

import com.apalombidev.schedorex.availability.AvailabilityService;

public record AvailabilityGenerationResponse(int created, int skipped) {

	public static AvailabilityGenerationResponse fromResult(AvailabilityService.GenerationResult result) {
		return new AvailabilityGenerationResponse(result.created(), result.skipped());
	}
}