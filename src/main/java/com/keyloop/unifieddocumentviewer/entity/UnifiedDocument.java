package com.keyloop.unifieddocumentviewer.entity;

import java.time.Instant;
import java.util.Objects;

public class UnifiedDocument {

	private final String id;
	private final String title;
	private final String type;
	private final String source;
	private final Instant createdAt;

	public UnifiedDocument(String id, String title, String type, String source, Instant createdAt) {
		this.id = id;
		this.title = title;
		this.type = type;
		this.source = source;
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}

	public String getSource() {
		return source;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UnifiedDocument that)) {
			return false;
		}
		return Objects.equals(id, that.id)
				&& Objects.equals(title, that.title)
				&& Objects.equals(type, that.type)
				&& Objects.equals(source, that.source)
				&& Objects.equals(createdAt, that.createdAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, title, type, source, createdAt);
	}
}
