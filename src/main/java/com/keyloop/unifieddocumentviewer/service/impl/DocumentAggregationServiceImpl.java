package com.keyloop.unifieddocumentviewer.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.keyloop.unifieddocumentviewer.constants.SourceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.keyloop.unifieddocumentviewer.dto.response.DocumentSearchResponse;
import com.keyloop.unifieddocumentviewer.dto.response.SourceResult;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.exception.DocumentNotAvailableException;
import com.keyloop.unifieddocumentviewer.exception.UpstreamDependencyException;
import com.keyloop.unifieddocumentviewer.exception.VehicleNotFoundException;
import com.keyloop.unifieddocumentviewer.service.DocumentAggregationService;
import com.keyloop.unifieddocumentviewer.service.DocumentSource;
import com.keyloop.unifieddocumentviewer.service.VehicleService;

@Service
public class DocumentAggregationServiceImpl implements DocumentAggregationService {

    private static final Duration SOURCE_TIMEOUT = Duration.ofSeconds(5);

	private final List<DocumentSource> documentSources;
	private final VehicleService vehicleService;
	private final ExecutorService executor;
	private final Duration sourceTimeout;

    @Autowired
    public DocumentAggregationServiceImpl(List<DocumentSource> documentSources, VehicleService vehicleService,
            @Qualifier("documentAggregationExecutor") ExecutorService executor) {
        this(documentSources, vehicleService, executor, SOURCE_TIMEOUT);
    }

	DocumentAggregationServiceImpl(List<DocumentSource> documentSources, VehicleService vehicleService,
			ExecutorService executor, Duration sourceTimeout) {
		this.documentSources = List.copyOf(documentSources);
		this.vehicleService = vehicleService;
		this.executor = executor;
		this.sourceTimeout = sourceTimeout;
	}

	@Override
	public DocumentSearchResponse searchDocumentsByVin(String vin) {
		String normalizedVin = vin.toUpperCase();
		SecurityContext securityContext = SecurityContextHolder.getContext();

		if (!vehicleService.existsByVin(normalizedVin)) {
			throw new VehicleNotFoundException("Vehicle not found for VIN " + normalizedVin + ".");
		}

		List<CompletableFuture<SourceResult>> futures = documentSources.stream()
				.map(source -> findDocumentsAsync(source, normalizedVin, securityContext))
				.toList();
		List<SourceResult> results = futures.stream()
				.map(CompletableFuture::join)
				.toList();

		if (results.stream().allMatch(SourceResult::failed)) {
			throw new UpstreamDependencyException("All document source systems are unavailable.");
		}

        List<UnifiedDocument> documents = new ArrayList<>();
		results.stream()
				.filter(result -> !result.failed())
				.map(SourceResult::documents)
				.forEach(documents::addAll);
        documents.sort(Comparator.comparing(UnifiedDocument::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

		if (documents.isEmpty()) {
			throw new DocumentNotAvailableException("Documents are not available for VIN " + normalizedVin + ".");
		}

		boolean partial = results.stream().anyMatch(SourceResult::failed);
		Map<String, SourceStatus> sources = results.stream()
				.collect(Collectors.toUnmodifiableMap(SourceResult::source, SourceResult::status));

        return new DocumentSearchResponse(normalizedVin, partial, sources, List.copyOf(documents));
    }

	private CompletableFuture<SourceResult> findDocumentsAsync(DocumentSource source, String normalizedVin,
			SecurityContext securityContext) {
		String sourceName = source.getSourceName();
		return CompletableFuture
				.supplyAsync(withSecurityContext(securityContext,
						() -> SourceResult.success(sourceName, source.findDocumentsByVin(normalizedVin))), executor)
				.completeOnTimeout(SourceResult.failed(sourceName), sourceTimeout.toMillis(), TimeUnit.MILLISECONDS)
				.exceptionally(exception -> SourceResult.failed(sourceName));
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
