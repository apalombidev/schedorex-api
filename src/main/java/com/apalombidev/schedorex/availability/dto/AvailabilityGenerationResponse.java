package com.apalombidev.schedorex.availability.dto;

import com.apalombidev.schedorex.availability.AvailabilityService;

/**
 * Avvolge AvailabilityService.GenerationResult (record interno, Step 5) in
 * un vero DTO REST — rimandato da v8 a "quando si disegna l'endpoint reale
 * di generazione slot" (Step 7, ora).
 */
public record AvailabilityGenerationResponse(int created, int skipped) {

    public static AvailabilityGenerationResponse fromResult(AvailabilityService.GenerationResult result) {
        return new AvailabilityGenerationResponse(result.created(), result.skipped());
    }
}