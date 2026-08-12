package com.keyloop.unifieddocumentviewer.dto.response;

import java.util.List;
import java.util.Map;

public record AuditLogLookupResponse(
		String requestId,
		Long durationMs,
		List<Map<String, Object>> records) {
}
