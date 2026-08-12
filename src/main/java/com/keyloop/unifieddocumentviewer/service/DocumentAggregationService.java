package com.keyloop.unifieddocumentviewer.service;

import com.keyloop.unifieddocumentviewer.dto.response.DocumentSearchResponse;

public interface DocumentAggregationService {

	DocumentSearchResponse searchDocumentsByVin(String vin);
}
