package com.apalombidev.schedorex.service.controller;

import com.apalombidev.schedorex.service.ServiceSetupService;
import com.apalombidev.schedorex.service.dto.ServiceResponse;
import com.apalombidev.schedorex.service.dto.ServiceSetupRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Setup/aggiornamento del servizio (pannello impostazioni admin). PUT,
 * non POST: setupOrUpdate() è un upsert idempotente su un singleton
 * (nessun id in ServiceSetupRequest, v7) — chiamarlo due volte con lo
 * stesso body produce lo stesso stato finale.
 */
@RestController
@RequestMapping("/api/admin/service")
@RequiredArgsConstructor
public class ServiceAdminController {

    private final ServiceSetupService serviceSetupService;

    @GetMapping
    public ServiceResponse getService() {
        return serviceSetupService.getService();
    }

    @PutMapping
    public ServiceResponse setupOrUpdate(@Valid @RequestBody ServiceSetupRequest request) {
        return serviceSetupService.setupOrUpdate(request);
    }
}