package com.keyloop.unifieddocumentviewer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ServiceDocumentServiceImplTest {

	private ServiceDocumentServiceImpl service;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		server = MockRestServiceServer.bindTo(restClientBuilder).build();
		service = new ServiceDocumentServiceImpl(restClientBuilder, "https://service.example.test");
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void findDocumentsByVinCallsServiceApiAndMapsDocuments() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				new AuthenticatedUser("user-123", "tenant-123"), null, List.of()));
		server.expect(once(), requestTo("https://service.example.test?vin=1HGCM82633A004352&tenantId=tenant-123"))
				.andRespond(withSuccess("""
						[
							{
								"id": "SERVICE-001",
								"documentName": "Annual Service Report",
								"documentType": "SERVICE_REPORT",
								"createdAt": "2026-07-15T11:00:00Z"
							}
						]
						""", MediaType.APPLICATION_JSON));

		List<UnifiedDocument> documents = service.findDocumentsByVin("1hgcm82633a004352");

		assertEquals(List.of(new UnifiedDocument(
				"SERVICE-001",
				"Annual Service Report",
				"SERVICE_REPORT",
				"SERVICE",
				Instant.parse("2026-07-15T11:00:00Z"))), documents);
		server.verify();
	}

	@Test
	void findDocumentsByVinReturnsEmptyListWhenVinIsNull() {
		assertTrue(service.findDocumentsByVin(null).isEmpty());
	}

	@Test
	void findDocumentsByVinReturnsEmptyListWhenVinIsBlank() {
		assertTrue(service.findDocumentsByVin(" ").isEmpty());
	}

	@Test
	void findDocumentsByVinReturnsEmptyListWhenServiceApiReturnsNullBody() {
		server.expect(once(), requestTo("https://service.example.test?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

		assertTrue(service.findDocumentsByVin("1HGCM82633A004352").isEmpty());
		server.verify();
	}

	@Test
	void findDocumentsByVinIgnoresNullItemsFromServiceApiResponse() {
		server.expect(once(), requestTo("https://service.example.test?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("""
						[
							null,
							{
								"id": "SERVICE-002",
								"documentName": "Repair Order",
								"documentType": "REPAIR_ORDER",
								"createdAt": "2026-07-16T11:00:00Z"
							}
						]
						""", MediaType.APPLICATION_JSON));

		List<UnifiedDocument> documents = service.findDocumentsByVin("1HGCM82633A004352");

		assertEquals(List.of(new UnifiedDocument(
				"SERVICE-002",
				"Repair Order",
				"REPAIR_ORDER",
				"SERVICE",
				Instant.parse("2026-07-16T11:00:00Z"))), documents);
		server.verify();
	}
}
