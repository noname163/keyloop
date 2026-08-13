package com.keyloop.unifieddocumentviewer.controller;

import com.keyloop.unifieddocumentviewer.dto.response.DocumentSearchResponse;
import com.keyloop.unifieddocumentviewer.service.DocumentAggregationService;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

	private static final String VIN_PATTERN = "^[A-HJ-NPR-Z0-9a-hj-npr-z0-9]{17}$";

	private final DocumentAggregationService documentAggregationService;

	public DocumentController(DocumentAggregationService documentAggregationService) {
		this.documentAggregationService = documentAggregationService;
	}

	@GetMapping("/vehicles/{vin}/documents")
	public DocumentSearchResponse searchDocuments(
			@PathVariable @Pattern(regexp = VIN_PATTERN, message = "The supplied VIN is invalid.") String vin) {
		return documentAggregationService.searchDocumentsByVin(vin);
	}
}
