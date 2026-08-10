package com.keyloop.unifieddocumentviewer.service;

import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;

public interface SalesDocumentService {

	List<UnifiedDocument> findDocumentsByVin(String vin, String accessToken);
}
