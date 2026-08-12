package com.keyloop.unifieddocumentviewer.service.impl;

import java.util.Objects;
import java.util.UUID;

import com.keyloop.unifieddocumentviewer.entity.Vehicle;
import com.keyloop.unifieddocumentviewer.repository.VehicleRepository;
import com.keyloop.unifieddocumentviewer.service.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class VehicleServiceImpl implements VehicleService {

	private final VehicleRepository vehicleRepository;

	public VehicleServiceImpl(VehicleRepository vehicleRepository) {
		this.vehicleRepository = vehicleRepository;
	}

	@Override
	public Page<Vehicle> getVehicles(UUID tenantId, Pageable pageable) {
		Objects.requireNonNull(tenantId, "tenantId is required");
		Objects.requireNonNull(pageable, "pageable is required");
		return vehicleRepository.findAllByTenantId(tenantId, pageable);
	}
}
