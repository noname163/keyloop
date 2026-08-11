package com.keyloop.unifieddocumentviewer.dto.response;

import java.time.Instant;

public record SourceDocumentResponse(
		String id,
		String documentName,
		String documentType,
		Instant createdAt) {
}
