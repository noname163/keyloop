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
import com.keyloop.unifieddocumentviewer.service.SalesDocumentService;
import com.keyloop.unifieddocumentviewer.service.ServiceDocumentService;
import com.keyloop.unifieddocumentviewer.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DocumentAggregationServiceImplTest {

    private static final String VIN = "1HGCM82633A004352";

    @Mock
    private SalesDocumentService salesDocumentService;

    @Mock
    private ServiceDocumentService serviceDocumentService;

    @Mock
    private VehicleService vehicleService;

    private ExecutorService executor;
    private DocumentAggregationServiceImpl service;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        service = new DocumentAggregationServiceImpl(salesDocumentService, serviceDocumentService, vehicleService,
                executor);
        when(vehicleService.existsByVin(VIN)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        executor.shutdownNow();
    }

    @Test
    void searchDocumentsByVinAggregatesAndSortsDocumentsDescendingByCreatedAt() {
        UnifiedDocument salesDocument = document("SALE-001", "SALES", "2026-07-01T09:00:00Z");
        UnifiedDocument serviceDocument = document("SERVICE-001", "SERVICE", "2026-07-15T11:00:00Z");
        when(salesDocumentService.findDocumentsByVin(VIN)).thenReturn(List.of(salesDocument));
        when(serviceDocumentService.findDocumentsByVin(VIN)).thenReturn(List.of(serviceDocument));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user-1", null, List.of()));

        DocumentSearchResponse response = service.searchDocumentsByVin("1hgcm82633a004352");

        assertEquals(VIN, response.vin());
        assertFalse(response.partial());
        assertEquals(SourceStatus.SUCCESS, response.sources().get("sales"));
        assertEquals(SourceStatus.SUCCESS, response.sources().get("service"));
        assertEquals(List.of(serviceDocument, salesDocument), response.documents());
    }

    @Test
    void searchDocumentsByVinReturnsEmptyDocumentsWhenBothSourcesReturnNoDocuments() {
        when(salesDocumentService.findDocumentsByVin(VIN)).thenReturn(List.of());
        when(serviceDocumentService.findDocumentsByVin(VIN)).thenReturn(List.of());

        assertThrows(DocumentNotAvailableException.class, () -> service.searchDocumentsByVin(VIN));
    }

    @Test
    void searchDocumentsByVinThrowsVehicleNotFoundBeforeCallingSourcesWhenVehicleDoesNotExist() {
        when(vehicleService.existsByVin(VIN)).thenReturn(false);

        assertThrows(VehicleNotFoundException.class, () -> service.searchDocumentsByVin(VIN));
    }

    @Test
    void searchDocumentsByVinReturnsPartialServiceDocumentsWhenSalesFails() {
        UnifiedDocument serviceDocument = document("SERVICE-001", "SERVICE", "2026-07-15T11:00:00Z");
        when(salesDocumentService.findDocumentsByVin(VIN)).thenThrow(new RuntimeException("sales down"));
        when(serviceDocumentService.findDocumentsByVin(VIN)).thenReturn(List.of(serviceDocument));

        DocumentSearchResponse response = service.searchDocumentsByVin(VIN);

        assertTrue(response.partial());
        assertEquals(SourceStatus.FAILED, response.sources().get("sales"));
        assertEquals(SourceStatus.SUCCESS, response.sources().get("service"));
        assertEquals(List.of(serviceDocument), response.documents());
    }

    @Test
    void searchDocumentsByVinReturnsPartialSalesDocumentsWhenServiceFails() {
        UnifiedDocument salesDocument = document("SALE-001", "SALES", "2026-07-01T09:00:00Z");
        when(salesDocumentService.findDocumentsByVin(VIN)).thenReturn(List.of(salesDocument));
        when(serviceDocumentService.findDocumentsByVin(VIN)).thenThrow(new RuntimeException("service down"));

        DocumentSearchResponse response = service.searchDocumentsByVin(VIN);

        assertTrue(response.partial());
        assertEquals(SourceStatus.SUCCESS, response.sources().get("sales"));
        assertEquals(SourceStatus.FAILED, response.sources().get("service"));
        assertEquals(List.of(salesDocument), response.documents());
    }

    @Test
    void searchDocumentsByVinReturnsPartialServiceDocumentsWhenSalesTimesOut() {
        service = new DocumentAggregationServiceImpl(salesDocumentService, serviceDocumentService, vehicleService,
                executor, Duration.ofMillis(50));
        UnifiedDocument serviceDocument = document("SERVICE-001", "SERVICE", "2026-07-15T11:00:00Z");
        when(salesDocumentService.findDocumentsByVin(VIN)).thenAnswer(invocation -> {
            Thread.sleep(500);
            return List.of(document("SALE-001", "SALES", "2026-07-01T09:00:00Z"));
        });
        when(serviceDocumentService.findDocumentsByVin(VIN)).thenReturn(List.of(serviceDocument));

        DocumentSearchResponse response = service.searchDocumentsByVin(VIN);

        assertTrue(response.partial());
        assertEquals(SourceStatus.FAILED, response.sources().get("sales"));
        assertEquals(SourceStatus.SUCCESS, response.sources().get("service"));
        assertEquals(List.of(serviceDocument), response.documents());
    }

    @Test
    void searchDocumentsByVinThrowsUpstreamDependencyExceptionWhenBothSourcesFail() {
        when(salesDocumentService.findDocumentsByVin(VIN)).thenThrow(new RuntimeException("sales down"));
        when(serviceDocumentService.findDocumentsByVin(VIN)).thenThrow(new RuntimeException("service down"));

        assertThrows(UpstreamDependencyException.class, () -> service.searchDocumentsByVin(VIN));
    }

    @Test
    void searchDocumentsByVinCallsSourceSystemsConcurrently() {
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        UnifiedDocument salesDocument = document("SALE-001", "SALES", "2026-07-01T09:00:00Z");
        when(salesDocumentService.findDocumentsByVin(VIN)).thenAnswer(invocation -> waitForOtherSource(bothStarted,
                release, salesDocument));
        when(serviceDocumentService.findDocumentsByVin(VIN)).thenAnswer(invocation -> waitForOtherSource(bothStarted,
                release));

        DocumentSearchResponse response = service.searchDocumentsByVin(VIN);

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
        when(salesDocumentService.findDocumentsByVin(VIN)).thenAnswer(invocation -> {
            salesPrincipal.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            return List.of(document("SALE-001", "SALES", "2026-07-01T09:00:00Z"));
        });
        when(serviceDocumentService.findDocumentsByVin(VIN)).thenAnswer(invocation -> {
            servicePrincipal.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            return List.of();
        });

        service.searchDocumentsByVin(VIN);

        assertEquals(authenticatedUser, salesPrincipal.get());
        assertEquals(authenticatedUser, servicePrincipal.get());
    }

    private List<UnifiedDocument> waitForOtherSource(CountDownLatch bothStarted, CountDownLatch release)
            throws InterruptedException {
        return waitForOtherSource(bothStarted, release, new UnifiedDocument[0]);
    }

    private List<UnifiedDocument> waitForOtherSource(CountDownLatch bothStarted, CountDownLatch release,
            UnifiedDocument... documents) throws InterruptedException {
        bothStarted.countDown();
        assertTrue(bothStarted.await(1, TimeUnit.SECONDS));
        release.countDown();
        assertTrue(release.await(1, TimeUnit.SECONDS));
        return List.of(documents);
    }

    private UnifiedDocument document(String id, String source, String createdAt) {
        return new UnifiedDocument(id, id + " title", id + " type", source, Instant.parse(createdAt));
    }

}
