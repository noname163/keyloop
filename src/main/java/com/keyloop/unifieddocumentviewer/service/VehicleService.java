package com.keyloop.unifieddocumentviewer.service;

import java.util.UUID;

import com.keyloop.unifieddocumentviewer.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleService {

	Page<Vehicle> getVehicles(UUID tenantId, Pageable pageable);
}
