package com.keyloop.unifieddocumentviewer.service.audit;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyloop.unifieddocumentviewer.exception.AuditLogLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogFileReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogFileReader.class);
	private static final TypeReference<Map<String, Object>> LOG_RECORD_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper objectMapper;

	public AuditLogFileReader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> matchingRecords(Path file, String requestId) {
		List<Map<String, Object>> records = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				parse(line, file).ifPresent(record -> {
					if (requestId.equals(record.get("requestId"))) {
						records.add(record);
					}
				});
			}
		}
		catch (NoSuchFileException exception) {
			LOGGER.debug("Audit log candidate file is not present");
		}
		catch (IOException exception) {
			throw new AuditLogLookupException("Audit log lookup failed.", exception);
		}
		return records;
	}

	private java.util.Optional<Map<String, Object>> parse(String line, Path file) {
		if (line == null || line.isBlank()) {
			return java.util.Optional.empty();
		}
		try {
			return java.util.Optional.of(objectMapper.readValue(line, LOG_RECORD_TYPE));
		}
		catch (IOException exception) {
			LOGGER.warn("Skipping malformed audit log line in {}", file.getFileName());
			return java.util.Optional.empty();
		}
	}
}
