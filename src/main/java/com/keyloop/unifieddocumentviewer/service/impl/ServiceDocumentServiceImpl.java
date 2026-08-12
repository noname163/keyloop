package com.keyloop.unifieddocumentviewer.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.keyloop.unifieddocumentviewer.dto.response.SourceDocumentResponse;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.security.AuthenticatedUser;
import com.keyloop.unifieddocumentviewer.service.ServiceDocumentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
	public List<UnifiedDocument> findDocumentsByVin(String vin) {
		if (vin == null || vin.isBlank()) {
			return List.of();
		}
		String normalizedVin = vin.toUpperCase();
		String tenantId = currentTenantId();

		SourceDocumentResponse[] response = restClient.get()
				.uri(uriBuilder -> {
					uriBuilder.queryParam("vin", normalizedVin);
					if (tenantId != null) {
						uriBuilder.queryParam("tenantId", tenantId);
					}
					return uriBuilder.build();
				})
				.retrieve()
				.body(SourceDocumentResponse[].class);

		if (response == null) {
			return List.of();
		}

		return Arrays.stream(response)
				.filter(Objects::nonNull)
				.map(document -> new UnifiedDocument(
						document.id(),
						document.documentName(),
						document.documentType(),
						"SERVICE",
						document.createdAt()))
				.toList();
	}

	private String currentTenantId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
			return null;
		}
		return authenticatedUser.tenantId();
	}
}
