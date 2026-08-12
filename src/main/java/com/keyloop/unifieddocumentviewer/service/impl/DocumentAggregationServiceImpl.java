package com.keyloop.unifieddocumentviewer.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.keyloop.unifieddocumentviewer.dto.response.DocumentSearchResponse;
import com.keyloop.unifieddocumentviewer.dto.response.SourceResult;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.exception.DocumentNotAvailableException;
import com.keyloop.unifieddocumentviewer.exception.UpstreamDependencyException;
import com.keyloop.unifieddocumentviewer.exception.VehicleNotFoundException;
import com.keyloop.unifieddocumentviewer.logging.ContextPropagatingExecutorService;
import com.keyloop.unifieddocumentviewer.service.DocumentAggregationService;
import com.keyloop.unifieddocumentviewer.service.SalesDocumentService;
import com.keyloop.unifieddocumentviewer.service.ServiceDocumentService;
import com.keyloop.unifieddocumentviewer.service.VehicleService;

import jakarta.annotation.PreDestroy;

@Service
public class DocumentAggregationServiceImpl implements DocumentAggregationService {

    private static final Duration SOURCE_TIMEOUT = Duration.ofSeconds(5);

	private final SalesDocumentService salesDocumentService;
	private final ServiceDocumentService serviceDocumentService;
	private final VehicleService vehicleService;
	private final ExecutorService executor;
	private final Duration sourceTimeout;

    @Autowired
    public DocumentAggregationServiceImpl(SalesDocumentService salesDocumentService,
            ServiceDocumentService serviceDocumentService, VehicleService vehicleService) {
        this(salesDocumentService, serviceDocumentService, vehicleService,
                new ContextPropagatingExecutorService(Executors.newFixedThreadPool(2)));
    }

    DocumentAggregationServiceImpl(SalesDocumentService salesDocumentService,
            ServiceDocumentService serviceDocumentService, VehicleService vehicleService, ExecutorService executor) {
        this(salesDocumentService, serviceDocumentService, vehicleService, executor, SOURCE_TIMEOUT);
    }

	DocumentAggregationServiceImpl(SalesDocumentService salesDocumentService,
			ServiceDocumentService serviceDocumentService, VehicleService vehicleService,
			ExecutorService executor, Duration sourceTimeout) {
		this.salesDocumentService = salesDocumentService;
		this.serviceDocumentService = serviceDocumentService;
		this.vehicleService = vehicleService;
		this.executor = executor;
		this.sourceTimeout = sourceTimeout;
	}

    @PreDestroy
    void shutdownExecutor() {
        executor.shutdown();
    }

	@Override
	public DocumentSearchResponse searchDocumentsByVin(String vin) {
		String normalizedVin = vin.toUpperCase();
		SecurityContext securityContext = SecurityContextHolder.getContext();

		if (!vehicleService.existsByVin(normalizedVin)) {
			throw new VehicleNotFoundException("Vehicle not found for VIN " + normalizedVin + ".");
		}

		CompletableFuture<SourceResult> salesFuture = CompletableFuture
				.supplyAsync(withSecurityContext(securityContext,
						() -> SourceResult.success("sales", salesDocumentService.findDocumentsByVin(normalizedVin))),
						executor)
				.completeOnTimeout(SourceResult.failed("sales"), sourceTimeout.toMillis(), TimeUnit.MILLISECONDS)
				.exceptionally(exception -> SourceResult.failed("sales"));
		CompletableFuture<SourceResult> serviceFuture = CompletableFuture
				.supplyAsync(withSecurityContext(securityContext,
						() -> SourceResult.success("service",
								serviceDocumentService.findDocumentsByVin(normalizedVin))), executor)
				.completeOnTimeout(SourceResult.failed("service"), sourceTimeout.toMillis(), TimeUnit.MILLISECONDS)
				.exceptionally(exception -> SourceResult.failed("service"));

        SourceResult sales = salesFuture.join();
        SourceResult service = serviceFuture.join();

		if (sales.failed() && service.failed()) {
			throw new UpstreamDependencyException("Both document source systems are unavailable.");
		}

        List<UnifiedDocument> documents = new ArrayList<>();
        documents.addAll(sales.documents());
        documents.addAll(service.documents());
        documents.sort(Comparator.comparing(UnifiedDocument::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

		if (documents.isEmpty()) {
			throw new DocumentNotAvailableException("Documents are not available for VIN " + normalizedVin + ".");
		}

		boolean partial = sales.failed() || service.failed();

        return new DocumentSearchResponse(normalizedVin, partial,
                Map.of("sales", sales.status(), "service", service.status()), List.copyOf(documents));
    }

	private <T> Supplier<T> withSecurityContext(SecurityContext securityContext, Supplier<T> supplier) {
		return () -> {
			SecurityContext previousContext = SecurityContextHolder.getContext();
			try {
				SecurityContextHolder.setContext(securityContext);
				return supplier.get();
			}
			finally {
				SecurityContextHolder.setContext(previousContext);
			}
		};
	}

}
