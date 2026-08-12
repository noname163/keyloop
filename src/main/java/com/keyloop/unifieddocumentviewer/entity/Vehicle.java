package com.keyloop.unifieddocumentviewer.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("vehicle")
public class Vehicle {

	@Id
	private UUID id;

	@Column("tenant_id")
	private String tenantId;

	private String vin;

	private String brand;

	private String model;

	@Column("registration_number")
	private String registrationNumber;

	@Column("created_at")
	private Instant createdAt;

	@Column("updated_at")
	private Instant updatedAt;

	public Vehicle(UUID id, String tenantId, String vin, String brand, String model, String registrationNumber,
			Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.tenantId = tenantId;
		this.vin = vin;
		this.brand = brand;
		this.model = model;
		this.registrationNumber = registrationNumber;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getVin() {
		return vin;
	}

	public String getBrand() {
		return brand;
	}

	public String getModel() {
		return model;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
