package com.keyloop.unifieddocumentviewer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

	@Test
	void findDocumentsByVinCallsServiceApiAndMapsDocuments() {
		server.expect(once(), requestTo("https://service.example.test/documents?vin=1HGCM82633A004352"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andRespond(withSuccess("""
						[
							{
								"id": "SERVICE-001",
								"title": "Annual Service Report",
								"type": "SERVICE_REPORT",
								"createdAt": "2026-07-15T11:00:00Z"
							}
						]
						""", MediaType.APPLICATION_JSON));

		List<UnifiedDocument> documents = service.findDocumentsByVin("1hgcm82633a004352", "access-token");

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
		assertTrue(service.findDocumentsByVin(null, "access-token").isEmpty());
	}

	@Test
	void findDocumentsByVinReturnsEmptyListWhenVinIsBlank() {
		assertTrue(service.findDocumentsByVin(" ", "access-token").isEmpty());
	}

	@Test
	void findDocumentsByVinReturnsEmptyListWhenServiceApiReturnsNullBody() {
		server.expect(once(), requestTo("https://service.example.test/documents?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

		assertTrue(service.findDocumentsByVin("1HGCM82633A004352", "access-token").isEmpty());
		server.verify();
	}

	@Test
	void findDocumentsByVinIgnoresNullItemsFromServiceApiResponse() {
		server.expect(once(), requestTo("https://service.example.test/documents?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("""
						[
							null,
							{
								"id": "SERVICE-002",
								"title": "Repair Order",
								"type": "REPAIR_ORDER",
								"createdAt": "2026-07-16T11:00:00Z"
							}
						]
						""", MediaType.APPLICATION_JSON));

		List<UnifiedDocument> documents = service.findDocumentsByVin("1HGCM82633A004352", null);

		assertEquals(List.of(new UnifiedDocument(
				"SERVICE-002",
				"Repair Order",
				"REPAIR_ORDER",
				"SERVICE",
				Instant.parse("2026-07-16T11:00:00Z"))), documents);
		server.verify();
	}
}
