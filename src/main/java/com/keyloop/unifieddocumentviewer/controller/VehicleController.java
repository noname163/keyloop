package com.keyloop.unifieddocumentviewer.controller;

import java.util.UUID;

import com.keyloop.unifieddocumentviewer.entity.Vehicle;
import com.keyloop.unifieddocumentviewer.service.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

	private final VehicleService vehicleService;

	public VehicleController(VehicleService vehicleService) {
		this.vehicleService = vehicleService;
	}

	@GetMapping
	public Page<Vehicle> getVehicles(
			@RequestHeader("X-Tenant-Id") UUID tenantId,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return vehicleService.getVehicles(tenantId, pageable);
	}
}
