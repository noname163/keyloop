package com.keyloop.unifieddocumentviewer.service.impl;

import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.service.SalesDocumentService;
import org.springframework.stereotype.Service;

@Service
public class SalesDocumentServiceImpl implements SalesDocumentService {

	@Override
	public List<UnifiedDocument> findDocumentsByVin(String vin) {
		// TODO: Implement Sales System document lookup.
		return List.of();
	}
}
