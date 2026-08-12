package com.keyloop.unifieddocumentviewer.service;

import com.keyloop.unifieddocumentviewer.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleService {

	Page<Vehicle> getVehicles(Pageable pageable);

	boolean existsByVin(String vin);
}
