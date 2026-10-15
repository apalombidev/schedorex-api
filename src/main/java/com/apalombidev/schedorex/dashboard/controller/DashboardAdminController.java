package com.apalombidev.schedorex.dashboard.controller;

import com.apalombidev.schedorex.availability.AvailabilityService;
import com.apalombidev.schedorex.dashboard.dto.DashboardSummaryResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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