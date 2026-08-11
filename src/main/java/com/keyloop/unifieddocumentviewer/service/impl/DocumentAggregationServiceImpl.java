package com.keyloop.unifieddocumentviewer.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.keyloop.unifieddocumentviewer.dto.response.DocumentSearchResponse;
import com.keyloop.unifieddocumentviewer.dto.response.SourceResult;
import com.keyloop.unifieddocumentviewer.entity.DocumentSearchAudit;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.exception.UpstreamDependencyException;
import com.keyloop.unifieddocumentviewer.service.AuditService;
import com.keyloop.unifieddocumentviewer.service.DocumentAggregationService;
import com.keyloop.unifieddocumentviewer.service.SalesDocumentService;
import com.keyloop.unifieddocumentviewer.service.ServiceDocumentService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DocumentAggregationServiceImpl implements DocumentAggregationService {

	private static final Duration SOURCE_TIMEOUT = Duration.ofSeconds(5);

	private final SalesDocumentService salesDocumentService;
	private final ServiceDocumentService serviceDocumentService;
	private final AuditService auditService;
	private final ExecutorService executor;
	private final Duration sourceTimeout;

	@Autowired
	public DocumentAggregationServiceImpl(SalesDocumentService salesDocumentService,
			ServiceDocumentService serviceDocumentService, AuditService auditService) {
		this(salesDocumentService, serviceDocumentService, auditService, Executors.newFixedThreadPool(2));
	}

	DocumentAggregationServiceImpl(SalesDocumentService salesDocumentService,
			ServiceDocumentService serviceDocumentService, AuditService auditService, ExecutorService executor) {
		this(salesDocumentService, serviceDocumentService, auditService, executor, SOURCE_TIMEOUT);
	}

	DocumentAggregationServiceImpl(SalesDocumentService salesDocumentService,
			ServiceDocumentService serviceDocumentService, AuditService auditService, ExecutorService executor,
			Duration sourceTimeout) {
		this.salesDocumentService = salesDocumentService;
		this.serviceDocumentService = serviceDocumentService;
		this.auditService = auditService;
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
		String searchedBy = currentUserId();
		Instant startedAt = Instant.now();

		CompletableFuture<SourceResult> salesFuture = CompletableFuture
				.supplyAsync(() -> SourceResult.success("sales", salesDocumentService.findDocumentsByVin(normalizedVin)),
						executor)
				.completeOnTimeout(SourceResult.failed("sales"), sourceTimeout.toMillis(), TimeUnit.MILLISECONDS)
				.exceptionally(exception -> SourceResult.failed("sales"));
		CompletableFuture<SourceResult> serviceFuture = CompletableFuture
				.supplyAsync(() -> SourceResult.success("service",
						serviceDocumentService.findDocumentsByVin(normalizedVin)), executor)
				.completeOnTimeout(SourceResult.failed("service"), sourceTimeout.toMillis(), TimeUnit.MILLISECONDS)
				.exceptionally(exception -> SourceResult.failed("service"));

		SourceResult sales = salesFuture.join();
		SourceResult service = serviceFuture.join();

		if (sales.failed() && service.failed()) {
			recordAudit(normalizedVin, searchedBy, startedAt, "FAILED", 0, 0);
			throw new UpstreamDependencyException("Both document source systems are unavailable.");
		}

		List<UnifiedDocument> documents = new ArrayList<>();
		documents.addAll(sales.documents());
		documents.addAll(service.documents());
		documents.sort(Comparator.comparing(UnifiedDocument::getCreatedAt,
				Comparator.nullsLast(Comparator.reverseOrder())));

		boolean partial = sales.failed() || service.failed();
		recordAudit(normalizedVin, searchedBy, startedAt, partial ? "PARTIAL" : "SUCCESS",
				sales.documents().size(), service.documents().size());

		return new DocumentSearchResponse(normalizedVin, partial,
				Map.of("sales", sales.status(), "service", service.status()), List.copyOf(documents));
	}

	private String currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? null : authentication.getName();
	}

	private void recordAudit(String vin, String searchedBy, Instant startedAt, String status,
			int salesCount, int serviceCount) {
		try {
			auditService.recordSearchAudit(new DocumentSearchAudit(vin, searchedBy, startedAt, status,
					salesCount, serviceCount, Duration.between(startedAt, Instant.now()).toMillis()));
		}
		catch (RuntimeException ignored) {
			// Audit failures should not mask document search results.
		}
	}

}
