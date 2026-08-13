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

class SalesDocumentServiceImplTest {

	private SalesDocumentServiceImpl service;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		server = MockRestServiceServer.bindTo(restClientBuilder).build();
		service = new SalesDocumentServiceImpl(restClientBuilder, "https://sales.example.test");
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void findDocumentsByVinCallsSalesApiAndMapsDocuments() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				new AuthenticatedUser("user-123", "tenant-123"), null, List.of()));
		server.expect(once(), requestTo("https://sales.example.test?vin=1HGCM82633A004352&tenantId=tenant-123"))
				.andRespond(withSuccess("""
						[
							{
								"id": "SALE-001",
								"documentName": "Purchase Agreement",
								"documentType": "SALES_CONTRACT",
								"createdAt": "2026-07-01T09:00:00Z"
							}
						]
						""", MediaType.APPLICATION_JSON));

		List<UnifiedDocument> documents = service.findDocumentsByVin("1hgcm82633a004352");

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
		assertTrue(service.findDocumentsByVin(null).isEmpty());
	}

	@Test
	void findDocumentsByVinReturnsEmptyListWhenVinIsBlank() {
		assertTrue(service.findDocumentsByVin(" ").isEmpty());
	}

	@Test
	void findDocumentsByVinReturnsEmptyListWhenSalesApiReturnsNullBody() {
		server.expect(once(), requestTo("https://sales.example.test?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

		assertTrue(service.findDocumentsByVin("1HGCM82633A004352").isEmpty());
		server.verify();
	}

	@Test
	void findDocumentsByVinOmitsTenantIdWhenPrincipalIsNotAuthenticatedUser() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"user-123", null, List.of()));
		server.expect(once(), requestTo("https://sales.example.test?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		assertTrue(service.findDocumentsByVin("1HGCM82633A004352").isEmpty());
		server.verify();
	}

	@Test
	void findDocumentsByVinIgnoresNullItemsFromSalesApiResponse() {
		server.expect(once(), requestTo("https://sales.example.test?vin=1HGCM82633A004352"))
				.andRespond(withSuccess("""
						[
							null,
							{
								"id": "SALE-002",
								"documentName": "Invoice",
								"documentType": "SALES_INVOICE",
								"createdAt": "2026-07-02T09:00:00Z"
							}
						]
						""", MediaType.APPLICATION_JSON));

		List<UnifiedDocument> documents = service.findDocumentsByVin("1HGCM82633A004352");

		assertEquals(List.of(new UnifiedDocument(
				"SALE-002",
				"Invoice",
				"SALES_INVOICE",
				"SALES",
				Instant.parse("2026-07-02T09:00:00Z"))), documents);
		server.verify();
	}
}
