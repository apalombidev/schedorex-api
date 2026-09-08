package com.apalombidev.schedorex.availability.controller;

import com.apalombidev.schedorex.availability.AvailabilityService;
import com.apalombidev.schedorex.availability.dto.AdminSlotResponse;
import com.apalombidev.schedorex.availability.dto.AvailabilityGenerationRequest;
import com.apalombidev.schedorex.availability.dto.AvailabilityGenerationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pannello "Genera disponibilità" (v2) + vista/gestione manuale slot
 * (blocco/sblocco, rimandato da v8).
 */
@RestController
@RequestMapping("/api/admin/availability")
@RequiredArgsConstructor
public class AvailabilityAdminController {

    private final AvailabilityService availabilityService;

    @PostMapping("/generate")
    public AvailabilityGenerationResponse generateSlots(
            @Valid @RequestBody AvailabilityGenerationRequest request) {
        AvailabilityService.GenerationResult result = availabilityService.generateSlots(request);
        return AvailabilityGenerationResponse.fromResult(result);
    }

    @GetMapping
    public List<AdminSlotResponse> getAdminSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return availabilityService.getAdminSlots(from, to);
    }

    @PostMapping("/{slotId}/block")
    public AdminSlotResponse blockSlot(@PathVariable UUID slotId) {
        return availabilityService.setSlotBlocked(slotId, true);
    }

    @PostMapping("/{slotId}/unblock")
    public AdminSlotResponse unblockSlot(@PathVariable UUID slotId) {
        return availabilityService.setSlotBlocked(slotId, false);
    }
}