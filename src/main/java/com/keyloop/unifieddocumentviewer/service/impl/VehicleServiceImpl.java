package com.keyloop.unifieddocumentviewer.service.impl;

import java.util.Objects;
import java.util.UUID;

import com.keyloop.unifieddocumentviewer.entity.Vehicle;
import com.keyloop.unifieddocumentviewer.repository.VehicleRepository;
import com.keyloop.unifieddocumentviewer.security.AuthenticatedUser;
import com.keyloop.unifieddocumentviewer.service.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class VehicleServiceImpl implements VehicleService {

	private final VehicleRepository vehicleRepository;

	public VehicleServiceImpl(VehicleRepository vehicleRepository) {
		this.vehicleRepository = vehicleRepository;
	}

	@Override
	public Page<Vehicle> getVehicles(Pageable pageable) {
		Objects.requireNonNull(pageable, "pageable is required");
		return vehicleRepository.findAllByTenantId(currentTenantId(), safePageable(pageable));
	}

	@Override
	public boolean existsByVin(String vin) {
		if (vin == null || vin.isBlank()) {
			return false;
		}
		return vehicleRepository.existsByVinAndTenantId(vin.toUpperCase(), currentTenantId());
	}

	private String currentTenantId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)
				|| authenticatedUser.tenantId() == null || authenticatedUser.tenantId().isBlank()) {
			throw new IllegalStateException("tenantId is required");
		}
		return authenticatedUser.tenantId();
	}

	private Pageable safePageable(Pageable pageable) {
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
	}
}
