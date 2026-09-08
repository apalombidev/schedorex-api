package com.apalombidev.schedorex.dashboard.controller;

import com.apalombidev.schedorex.availability.AvailabilityService;
import com.apalombidev.schedorex.dashboard.dto.DashboardSummaryResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Banner dashboard "meno di 7 giorni di slot futuri" (v2, rimandato da v8). */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardAdminController {

    private final AvailabilityService availabilityService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return availabilityService.getDashboardSummary();
    }
}