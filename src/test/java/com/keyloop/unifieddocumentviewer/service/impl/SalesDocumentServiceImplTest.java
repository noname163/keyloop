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

class SalesDocumentServiceImplTest {

	private SalesDocumentServiceImpl service;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		server = MockRestServiceServer.bindTo(restClientBuilder).build();
		service = new SalesDocumentServiceImpl(restClientBuilder, "https://sales.example.test");
	}

	@Test
	void findDocumentsByVinCallsSalesApiAndMapsDocuments() {
		server.expect(once(), requestTo("https://sales.example.test/documents?vin=1HGCM82633A004352"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andRespond(withSuccess("""
						[
							{
								"id": "SALE-001",
								"title": "Purchase Agreement",
								"type": "SALES_CONTRACT",
								"createdAt": "2026-07-01T09:00:00Z"
							}
						]
						""", MediaType.APPLICATION_JSON));

		List<UnifiedDocument> documents = service.findDocumentsByVin("1hgcm82633a004352", "access-token");

		assertEquals(List.of(new UnifiedDocument(
				"SALE-001",
				"Purchase Agreement",
				"SALES_CONTRACT",
				"SALES",
				Instant.parse("2026-07-01T09:00:00Z"))), documents);
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
	void findDocumentsByVinReturnsEmptyListWhenSalesApiReturnsNullBody() {
		server.expect(once(), requestTo("https://sales.example.test/documents?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

		assertTrue(service.findDocumentsByVin("1HGCM82633A004352", "access-token").isEmpty());
		server.verify();
	}

	@Test
	void findDocumentsByVinIgnoresNullItemsFromSalesApiResponse() {
		server.expect(once(), requestTo("https://sales.example.test/documents?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("""
						[
							null,
							{
								"id": "SALE-002",
								"title": "Invoice",
								"type": "SALES_INVOICE",
								"createdAt": "2026-07-02T09:00:00Z"
							}
						]
						""", MediaType.APPLICATION_JSON));

		List<UnifiedDocument> documents = service.findDocumentsByVin("1HGCM82633A004352", null);

		assertEquals(List.of(new UnifiedDocument(
				"SALE-002",
				"Invoice",
				"SALES_INVOICE",
				"SALES",
				Instant.parse("2026-07-02T09:00:00Z"))), documents);
		server.verify();
	}
}
