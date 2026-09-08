package com.apalombidev.schedorex.service.controller;

import com.apalombidev.schedorex.service.ServiceSetupService;
import com.apalombidev.schedorex.service.dto.ServiceResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vista pubblica del servizio: il cliente deve conoscere nome, durata,
 * prezzo, valuta, lingua e se è richiesto un pagamento prima di prenotare.
 */
@RestController
@RequestMapping("/api/public/service")
@RequiredArgsConstructor
public class ServicePublicController {

    private final ServiceSetupService serviceSetupService;

    @GetMapping
    public ServiceResponse getService() {
        return serviceSetupService.getService();
    }
}