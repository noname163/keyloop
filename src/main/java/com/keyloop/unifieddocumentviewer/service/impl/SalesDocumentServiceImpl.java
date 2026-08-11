package com.keyloop.unifieddocumentviewer.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.keyloop.unifieddocumentviewer.dto.SourceDocumentResponse;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.service.SalesDocumentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SalesDocumentServiceImpl implements SalesDocumentService {

	private final RestClient restClient;

	public SalesDocumentServiceImpl(RestClient.Builder restClientBuilder,
			@Value("${sales.system.base-url}") String salesSystemBaseUrl) {
		this.restClient = restClientBuilder.baseUrl(salesSystemBaseUrl).build();
	}

	@Override
	public List<UnifiedDocument> findDocumentsByVin(String vin, String accessToken) {
		if (vin == null || vin.isBlank()) {
			return List.of();
		}
		String normalizedVin = vin.toUpperCase();

		SourceDocumentResponse[] response = restClient.get()
				.uri(uriBuilder -> uriBuilder.path("/documents").queryParam("vin", normalizedVin).build())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.body(SourceDocumentResponse[].class);

		if (response == null) {
			return List.of();
		}

		return Arrays.stream(response)
				.filter(Objects::nonNull)
				.map(document -> new UnifiedDocument(
						document.id(),
						document.title(),
						document.type(),
						"SALES",
						document.createdAt()))
				.toList();
	}
}
