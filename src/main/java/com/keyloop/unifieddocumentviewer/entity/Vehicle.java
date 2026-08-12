package com.keyloop.unifieddocumentviewer.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("vehicle")
public record Vehicle(
		@Id UUID id,
		@Column("tenant_id") UUID tenantId,
		String vin,
		String brand,
		String model,
		@Column("registration_number") String registrationNumber,
		@Column("created_at") Instant createdAt,
		@Column("updated_at") Instant updatedAt) {
}
