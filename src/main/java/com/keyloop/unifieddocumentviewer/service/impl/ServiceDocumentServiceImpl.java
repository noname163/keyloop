package com.keyloop.unifieddocumentviewer.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.keyloop.unifieddocumentviewer.dto.SourceDocumentResponse;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.service.ServiceDocumentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ServiceDocumentServiceImpl implements ServiceDocumentService {

	private final RestClient restClient;

	public ServiceDocumentServiceImpl(RestClient.Builder restClientBuilder,
			@Value("${service.system.base-url}") String serviceSystemBaseUrl) {
		this.restClient = restClientBuilder.baseUrl(serviceSystemBaseUrl).build();
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
						"SERVICE",
						document.createdAt()))
				.toList();
	}
}
