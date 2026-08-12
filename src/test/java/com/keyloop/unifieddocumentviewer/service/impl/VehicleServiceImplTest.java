package com.keyloop.unifieddocumentviewer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.keyloop.unifieddocumentviewer.entity.Vehicle;
import com.keyloop.unifieddocumentviewer.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class VehicleServiceImplTest {

	private final VehicleRepository repository = mock(VehicleRepository.class);
	private final VehicleServiceImpl service = new VehicleServiceImpl(repository);

	@Test
	void getVehiclesUsesTenantFilteredRepositoryQuery() {
		UUID tenantId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(1, 10);
		Vehicle vehicle = new Vehicle(UUID.randomUUID(), tenantId, "1HGCM82633A004352", "Honda", "Accord",
				"ABC-123", Instant.parse("2026-08-01T10:00:00Z"), Instant.parse("2026-08-01T10:00:00Z"));
		Page<Vehicle> result = new PageImpl<>(List.of(vehicle), pageable, 11);
		when(repository.findAllByTenantId(tenantId, pageable)).thenReturn(result);

		assertEquals(result, service.getVehicles(tenantId, pageable));
		verify(repository).findAllByTenantId(tenantId, pageable);
	}

	@Test
	void getVehiclesRejectsMissingTenant() {
		assertThrows(NullPointerException.class, () -> service.getVehicles(null, PageRequest.of(0, 20)));
	}

	@Test
	void getVehiclesRejectsMissingPageable() {
		assertThrows(NullPointerException.class, () -> service.getVehicles(UUID.randomUUID(), null));
	}
}
