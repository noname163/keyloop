package com.keyloop.unifieddocumentviewer.service;

import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;

public interface SalesDocumentService extends DocumentSource {

	List<UnifiedDocument> findDocumentsByVin(String vin);
}
