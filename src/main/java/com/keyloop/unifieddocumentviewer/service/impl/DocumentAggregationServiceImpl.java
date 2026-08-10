package com.keyloop.unifieddocumentviewer.service.impl;

import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.service.DocumentAggregationService;
import org.springframework.stereotype.Service;

@Service
public class DocumentAggregationServiceImpl implements DocumentAggregationService {

	@Override
	public List<UnifiedDocument> searchDocumentsByVin(String vin, String requestId, String searchedBy) {
		// TODO: Implement document aggregation workflow.
		return List.of();
	}
}
