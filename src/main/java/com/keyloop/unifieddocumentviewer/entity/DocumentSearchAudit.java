package com.keyloop.unifieddocumentviewer.entity;

import java.time.Instant;

public class DocumentSearchAudit {

	private final String vin;
	private final String searchedBy;
	private final Instant searchedAt;
	private final String status;
	private final int salesResultCount;
	private final int serviceResultCount;
	private final long durationMs;

	public DocumentSearchAudit(String vin, String searchedBy, Instant searchedAt, String status,
			int salesResultCount, int serviceResultCount, long durationMs) {
		this.vin = vin;
		this.searchedBy = searchedBy;
		this.searchedAt = searchedAt;
		this.status = status;
		this.salesResultCount = salesResultCount;
		this.serviceResultCount = serviceResultCount;
		this.durationMs = durationMs;
	}

	public String getVin() {
		return vin;
	}

	public String getSearchedBy() {
		return searchedBy;
	}

	public Instant getSearchedAt() {
		return searchedAt;
	}

	public String getStatus() {
		return status;
	}

	public int getSalesResultCount() {
		return salesResultCount;
	}

	public int getServiceResultCount() {
		return serviceResultCount;
	}

	public long getDurationMs() {
		return durationMs;
	}
}
