package com.apalombidev.schedorex.service;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalServiceRepository extends JpaRepository<ProfessionalService, UUID> {

}
