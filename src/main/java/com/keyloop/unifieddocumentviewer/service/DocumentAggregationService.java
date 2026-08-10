package com.keyloop.unifieddocumentviewer.service;

import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;

public interface DocumentAggregationService {

	List<UnifiedDocument> searchDocumentsByVin(String vin, String requestId, String searchedBy);
}
