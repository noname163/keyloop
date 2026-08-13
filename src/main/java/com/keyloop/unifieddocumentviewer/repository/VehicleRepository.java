package com.keyloop.unifieddocumentviewer.repository;

import java.util.UUID;

import com.keyloop.unifieddocumentviewer.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface VehicleRepository extends Repository<Vehicle, UUID> {

	Page<Vehicle> findAllByTenantId(String tenantId, Pageable pageable);

	boolean existsByVinAndTenantId(String vin, String tenantId);
}
