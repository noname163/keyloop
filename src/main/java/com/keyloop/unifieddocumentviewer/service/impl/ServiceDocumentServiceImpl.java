package com.keyloop.unifieddocumentviewer.service.impl;

import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.service.ServiceDocumentService;
import org.springframework.stereotype.Service;

@Service
public class ServiceDocumentServiceImpl implements ServiceDocumentService {

	@Override
	public List<UnifiedDocument> findDocumentsByVin(String vin) {
		// TODO: Implement Service System document lookup.
		return List.of();
	}
}
