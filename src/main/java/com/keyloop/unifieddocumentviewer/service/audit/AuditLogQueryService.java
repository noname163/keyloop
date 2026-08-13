package com.keyloop.unifieddocumentviewer.service.audit;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.keyloop.unifieddocumentviewer.dto.response.AuditLogLookupResponse;
import com.keyloop.unifieddocumentviewer.exception.AuditLogNotFoundException;
import com.keyloop.unifieddocumentviewer.exception.InvalidRequestIdException;
import org.springframework.stereotype.Service;

@Service
public class AuditLogQueryService {

	private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

	private final AuditLogQueryProperties properties;
	private final AuditLogFileReader fileReader;

	public AuditLogQueryService(AuditLogQueryProperties properties, AuditLogFileReader fileReader) {
		this.properties = properties;
		this.fileReader = fileReader;
	}

	public AuditLogLookupResponse findByRequestId(String requestId) {
		validate(requestId);
		List<Map<String, Object>> records = candidateFiles().stream()
				.flatMap(file -> fileReader.matchingRecords(file, requestId).stream())
				.sorted(Comparator.comparing(this::recordInstant))
				.collect(Collectors.toList());

		if (records.isEmpty()) {
			throw new AuditLogNotFoundException(requestId);
		}
		return new AuditLogLookupResponse(requestId, durationMs(records), records);
	}

	private Long durationMs(List<Map<String, Object>> records) {
		Instant earliestStart = null;
		Instant latestEnd = null;
		Long maxRecordDuration = null;
		for (Map<String, Object> record : records) {
			Instant start = instant(record.get("startTime"));
			if (start != null && (earliestStart == null || start.isBefore(earliestStart))) {
				earliestStart = start;
			}
			Instant end = instant(record.get("endTime"));
			if (end != null && (latestEnd == null || end.isAfter(latestEnd))) {
				latestEnd = end;
			}
			Long recordDuration = longValue(record.get("durationMs"));
			if (recordDuration != null && (maxRecordDuration == null || recordDuration > maxRecordDuration)) {
				maxRecordDuration = recordDuration;
			}
		}
		if (earliestStart != null && latestEnd != null && !latestEnd.isBefore(earliestStart)) {
			return Duration.between(earliestStart, latestEnd).toMillis();
		}
		return maxRecordDuration;
	}

	private Instant instant(Object value) {
		if (value instanceof String text) {
			Instant instant = parseInstant(text);
			return Instant.EPOCH.equals(instant) ? null : instant;
		}
		return null;
	}

	private Long longValue(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value instanceof String text) {
			try {
				return Long.parseLong(text);
			}
			catch (NumberFormatException exception) {
				return null;
			}
		}
		return null;
	}

	private void validate(String requestId) {
		if (requestId == null || !REQUEST_ID_PATTERN.matcher(requestId).matches()) {
			throw new InvalidRequestIdException();
		}
	}

	private Set<Path> candidateFiles() {
		Path logDirectory = Path.of(properties.logPath(), "audit").normalize();
		String applicationName = properties.applicationName();
		Set<Path> files = new LinkedHashSet<>();
		IntStream.range(0, properties.maxDays())
				.mapToObj(LocalDate.now()::minusDays)
				.map(date -> applicationName + "-audit-" + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log")
				.map(logDirectory::resolve)
				.map(Path::normalize)
				.forEach(files::add);
		return files;
	}

	private Instant recordInstant(Map<String, Object> record) {
		Object startTime = record.get("startTime");
		if (startTime instanceof String value) {
			return parseInstant(value);
		}
		Object timestamp = record.get("timestamp");
		if (timestamp instanceof String value) {
			return parseInstant(value);
		}
		return Instant.EPOCH;
	}

	private Instant parseInstant(String value) {
		try {
			return Instant.parse(value);
		}
		catch (RuntimeException exception) {
			return Instant.EPOCH;
		}
	}
}
