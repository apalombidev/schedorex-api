package com.apalombidev.schedorex.availability.controller;

import com.apalombidev.schedorex.availability.AvailabilityService;
import com.apalombidev.schedorex.availability.dto.AvailableSlotResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Query pubblica "slot liberi per servizio in un range di date" (Step 9,
 * punto 3 del flusso end-to-end). from/to sono Instant in formato ISO-8601
 * (es. 2026-09-10T09:00:00Z) — nessun default: il chiamante (frontend,
 * Fase 2) decide il range da mostrare.
 */
@RestController
@RequestMapping("/api/public/availability")
@RequiredArgsConstructor
public class AvailabilityPublicController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public List<AvailableSlotResponse> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return availabilityService.getAvailableSlots(from, to);
    }
}