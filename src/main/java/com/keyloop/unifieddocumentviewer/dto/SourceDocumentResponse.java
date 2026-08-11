package com.keyloop.unifieddocumentviewer.dto;

import java.time.Instant;

public record SourceDocumentResponse(
		String id,
		String title,
		String type,
		Instant createdAt) {
}
