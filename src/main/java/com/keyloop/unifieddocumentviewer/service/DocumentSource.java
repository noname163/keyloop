package com.keyloop.unifieddocumentviewer.service;

import java.util.List;

import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;

public interface DocumentSource {

	String getSourceName();

	List<UnifiedDocument> findDocumentsByVin(String vin);
}
