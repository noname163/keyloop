package com.keyloop.unifieddocumentviewer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.keyloop.unifieddocumentviewer.entity.Vehicle;
import com.keyloop.unifieddocumentviewer.repository.VehicleRepository;
import com.keyloop.unifieddocumentviewer.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class VehicleServiceImplTest {

	private final VehicleRepository repository = mock(VehicleRepository.class);
	private final VehicleServiceImpl service = new VehicleServiceImpl(repository);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getVehiclesUsesTenantFilteredRepositoryQuery() {
		String tenantId = UUID.randomUUID().toString();
		setTenantId(tenantId);
		Pageable pageable = PageRequest.of(1, 10, Sort.by("createdAt: DESC"));
		Pageable safePageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "id"));
		Vehicle vehicle = new Vehicle(UUID.randomUUID(), tenantId, "1HGCM82633A004352", "Honda", "Accord",
				"ABC-123", Instant.parse("2026-08-01T10:00:00Z"), Instant.parse("2026-08-01T10:00:00Z"));
		Page<Vehicle> result = new PageImpl<>(List.of(vehicle), safePageable, 11);
		when(repository.findAllByTenantId(tenantId, safePageable)).thenReturn(result);

		assertEquals(result, service.getVehicles(pageable));
		verify(repository).findAllByTenantId(tenantId, safePageable);
	}

	@Test
	void getVehiclesRejectsMissingTenantInSecurityContext() {
		assertThrows(IllegalStateException.class, () -> service.getVehicles(PageRequest.of(0, 20)));
	}

	@Test
	void getVehiclesRejectsMissingPageable() {
		setTenantId(UUID.randomUUID().toString());
		assertThrows(NullPointerException.class, () -> service.getVehicles(null));
	}

	@Test
	void existsByVinUsesTenantFilteredRepositoryQuery() {
		String tenantId = UUID.randomUUID().toString();
		setTenantId(tenantId);
		when(repository.existsByVinAndTenantId("1HGCM82633A004352", tenantId)).thenReturn(true);

		assertTrue(service.existsByVin("1hgcm82633a004352"));
		verify(repository).existsByVinAndTenantId("1HGCM82633A004352", tenantId);
	}

	@Test
	void existsByVinReturnsFalseForBlankVin() {
		assertFalse(service.existsByVin(" "));
		verify(repository, never()).existsByVinAndTenantId(null, null);
	}

	@Test
	void existsByVinRejectsMissingTenantInSecurityContext() {
		assertThrows(IllegalStateException.class, () -> service.existsByVin("1HGCM82633A004352"));
	}

	private void setTenantId(String tenantId) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				new AuthenticatedUser("user-123", tenantId), null, List.of()));
	}
}
