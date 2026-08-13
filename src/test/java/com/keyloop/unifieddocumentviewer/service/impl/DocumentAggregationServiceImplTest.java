package com.keyloop.unifieddocumentviewer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.keyloop.unifieddocumentviewer.constants.SourceStatus;
import com.keyloop.unifieddocumentviewer.dto.response.DocumentSearchResponse;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.exception.DocumentNotAvailableException;
import com.keyloop.unifieddocumentviewer.exception.UpstreamDependencyException;
import com.keyloop.unifieddocumentviewer.exception.VehicleNotFoundException;
import com.keyloop.unifieddocumentviewer.security.AuthenticatedUser;
import com.keyloop.unifieddocumentviewer.service.DocumentSource;
import com.keyloop.unifieddocumentviewer.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DocumentAggregationServiceImplTest {

	private static final String VIN = "1HGCM82633A004352";

	@Mock
	private VehicleService vehicleService;

	private ExecutorService executor;

	@BeforeEach
	void setUp() {
		executor = Executors.newFixedThreadPool(4);
		when(vehicleService.existsByVin(VIN)).thenReturn(true);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		executor.shutdownNow();
	}

	@Test
	void searchDocumentsByVinReturnsAllSourceStatusesWhenAllSourcesSucceed() {
		UnifiedDocument salesDocument = document("SALE-001", "SALES", "2026-07-01T09:00:00Z");
		UnifiedDocument serviceDocument = document("SERVICE-001", "SERVICE", "2026-07-15T11:00:00Z");

		DocumentSearchResponse response = service(
				source("sales", List.of(salesDocument)),
				source("service", List.of(serviceDocument)))
				.searchDocumentsByVin("1hgcm82633a004352");

		assertEquals(VIN, response.vin());
		assertFalse(response.partial());
		assertEquals(SourceStatus.SUCCESS, response.sources().get("sales"));
		assertEquals(SourceStatus.SUCCESS, response.sources().get("service"));
	}

	@Test
	void searchDocumentsByVinReturnsPartialDocumentsWhenOneSourceFails() {
		UnifiedDocument serviceDocument = document("SERVICE-001", "SERVICE", "2026-07-15T11:00:00Z");

		DocumentSearchResponse response = service(
				failingSource("sales"),
				source("service", List.of(serviceDocument)))
				.searchDocumentsByVin(VIN);

		assertTrue(response.partial());
		assertEquals(SourceStatus.FAILED, response.sources().get("sales"));
		assertEquals(SourceStatus.SUCCESS, response.sources().get("service"));
		assertEquals(List.of(serviceDocument), response.documents());
	}

	@Test
	void searchDocumentsByVinThrowsUpstreamDependencyExceptionWhenAllSourcesFail() {
		DocumentAggregationServiceImpl service = service(failingSource("sales"), failingSource("service"));

		assertThrows(UpstreamDependencyException.class, () -> service.searchDocumentsByVin(VIN));
	}

	@Test
	void searchDocumentsByVinReturnsPartialDocumentsWhenSourceTimesOut() {
		DocumentAggregationServiceImpl service = service(Duration.ofMillis(50),
				slowSource("sales", 500, List.of(document("SALE-001", "SALES", "2026-07-01T09:00:00Z"))),
				source("service", List.of(document("SERVICE-001", "SERVICE", "2026-07-15T11:00:00Z"))));

		DocumentSearchResponse response = service.searchDocumentsByVin(VIN);

		assertTrue(response.partial());
		assertEquals(SourceStatus.FAILED, response.sources().get("sales"));
		assertEquals(SourceStatus.SUCCESS, response.sources().get("service"));
		assertEquals(List.of(document("SERVICE-001", "SERVICE", "2026-07-15T11:00:00Z")), response.documents());
	}

	@Test
	void searchDocumentsByVinThrowsDocumentNotAvailableExceptionWhenAllSuccessfulSourcesReturnNoDocuments() {
		DocumentAggregationServiceImpl service = service(source("sales", List.of()), source("service", List.of()));

		assertThrows(DocumentNotAvailableException.class, () -> service.searchDocumentsByVin(VIN));
	}

	@Test
	void searchDocumentsByVinCombinesAndSortsDocumentsFromMultipleSourcesDescendingByCreatedAtWithNullsLast() {
		UnifiedDocument oldDocument = document("SALE-001", "SALES", "2026-07-01T09:00:00Z");
		UnifiedDocument newDocument = document("SERVICE-001", "SERVICE", "2026-07-15T11:00:00Z");
		UnifiedDocument nullDateDocument = new UnifiedDocument("FIN-001", "Finance", "TYPE", "FINANCE", null);

		DocumentSearchResponse response = service(
				source("sales", List.of(oldDocument)),
				source("service", List.of(newDocument)),
				source("finance", List.of(nullDateDocument)))
				.searchDocumentsByVin(VIN);

		assertEquals(List.of(newDocument, oldDocument, nullDateDocument), response.documents());
	}

	@Test
	void searchDocumentsByVinBuildsSourceStatusMapDynamically() {
		DocumentSearchResponse response = service(
				source("sales", List.of(document("SALE-001", "SALES", "2026-07-01T09:00:00Z"))),
				failingSource("insurance"),
				source("finance", List.of()))
				.searchDocumentsByVin(VIN);

		assertEquals(3, response.sources().size());
		assertEquals(SourceStatus.SUCCESS, response.sources().get("sales"));
		assertEquals(SourceStatus.FAILED, response.sources().get("insurance"));
		assertEquals(SourceStatus.SUCCESS, response.sources().get("finance"));
	}

	@Test
	void searchDocumentsByVinIncludesThirdDocumentSourceWithoutAggregationServiceChanges() {
		UnifiedDocument warrantyDocument = document("WAR-001", "WARRANTY", "2026-08-01T12:00:00Z");

		DocumentSearchResponse response = service(
				source("sales", List.of()),
				source("service", List.of()),
				source("warranty", List.of(warrantyDocument)))
				.searchDocumentsByVin(VIN);

		assertFalse(response.partial());
		assertEquals(SourceStatus.SUCCESS, response.sources().get("warranty"));
		assertEquals(List.of(warrantyDocument), response.documents());
	}

	@Test
	void searchDocumentsByVinThrowsVehicleNotFoundBeforeCallingSourcesWhenVehicleDoesNotExist() {
		when(vehicleService.existsByVin(VIN)).thenReturn(false);

		assertThrows(VehicleNotFoundException.class, () -> service(source("sales", List.of())).searchDocumentsByVin(VIN));
	}

	@Test
	void searchDocumentsByVinCallsSourceSystemsConcurrently() {
		CountDownLatch bothStarted = new CountDownLatch(2);
		CountDownLatch release = new CountDownLatch(1);
		UnifiedDocument salesDocument = document("SALE-001", "SALES", "2026-07-01T09:00:00Z");

		DocumentSearchResponse response = service(
				waitingSource("sales", bothStarted, release, List.of(salesDocument)),
				waitingSource("service", bothStarted, release, List.of()))
				.searchDocumentsByVin(VIN);

		assertFalse(response.partial());
		assertEquals(List.of(salesDocument), response.documents());
	}

	@Test
	void searchDocumentsByVinPropagatesSecurityContextToSourceThreads() {
		AtomicReference<Object> salesPrincipal = new AtomicReference<>();
		AtomicReference<Object> servicePrincipal = new AtomicReference<>();
		AuthenticatedUser authenticatedUser = new AuthenticatedUser("user-1", "tenant-1");
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of()));

		service(
				observingSource("sales", salesPrincipal, List.of(document("SALE-001", "SALES", "2026-07-01T09:00:00Z"))),
				observingSource("service", servicePrincipal, List.of()))
				.searchDocumentsByVin(VIN);

		assertEquals(authenticatedUser, salesPrincipal.get());
		assertEquals(authenticatedUser, servicePrincipal.get());
	}

	private DocumentAggregationServiceImpl service(DocumentSource... sources) {
		return service(SOURCE_TIMEOUT_FOR_TESTS, sources);
	}

	private DocumentAggregationServiceImpl service(Duration sourceTimeout, DocumentSource... sources) {
		return new DocumentAggregationServiceImpl(List.of(sources), vehicleService, executor, sourceTimeout);
	}

	private DocumentSource source(String name, List<UnifiedDocument> documents) {
		return new StubDocumentSource(name, documents);
	}

	private DocumentSource failingSource(String name) {
		return new StubDocumentSource(name, new RuntimeException(name + " down"));
	}

	private DocumentSource slowSource(String name, long sleepMillis, List<UnifiedDocument> documents) {
		return new StubDocumentSource(name, vin -> {
			Thread.sleep(sleepMillis);
			return documents;
		});
	}

	private DocumentSource waitingSource(String name, CountDownLatch bothStarted, CountDownLatch release,
			List<UnifiedDocument> documents) {
		return new StubDocumentSource(name, vin -> {
			bothStarted.countDown();
			assertTrue(bothStarted.await(1, TimeUnit.SECONDS));
			release.countDown();
			assertTrue(release.await(1, TimeUnit.SECONDS));
			return documents;
		});
	}

	private DocumentSource observingSource(String name, AtomicReference<Object> principal,
			List<UnifiedDocument> documents) {
		return new StubDocumentSource(name, vin -> {
			principal.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
			return documents;
		});
	}

	private UnifiedDocument document(String id, String source, String createdAt) {
		return new UnifiedDocument(id, id + " title", id + " type", source, Instant.parse(createdAt));
	}

	private static final Duration SOURCE_TIMEOUT_FOR_TESTS = Duration.ofSeconds(5);

	private interface SourceCall {

		List<UnifiedDocument> findDocumentsByVin(String vin) throws Exception;
	}

	private static class StubDocumentSource implements DocumentSource {

		private final String name;
		private final SourceCall sourceCall;

		StubDocumentSource(String name, List<UnifiedDocument> documents) {
			this(name, vin -> documents);
		}

		StubDocumentSource(String name, RuntimeException exception) {
			this(name, vin -> {
				throw exception;
			});
		}

		StubDocumentSource(String name, SourceCall sourceCall) {
			this.name = name;
			this.sourceCall = sourceCall;
		}

		@Override
		public String getSourceName() {
			return name;
		}

		@Override
		public List<UnifiedDocument> findDocumentsByVin(String vin) {
			try {
				return sourceCall.findDocumentsByVin(vin);
			}
			catch (RuntimeException exception) {
				throw exception;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}
	}
}
