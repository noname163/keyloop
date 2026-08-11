package com.keyloop.unifieddocumentviewer.dto.response;

import java.util.List;
import java.util.Map;

import com.keyloop.unifieddocumentviewer.constants.SourceStatus;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;

public record DocumentSearchResponse(
		String vin,
		boolean partial,
		Map<String, SourceStatus> sources,
		List<UnifiedDocument> documents) {
}
