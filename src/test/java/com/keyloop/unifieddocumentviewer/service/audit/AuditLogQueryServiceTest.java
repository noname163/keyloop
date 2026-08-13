package com.keyloop.unifieddocumentviewer.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyloop.unifieddocumentviewer.dto.response.AuditLogLookupResponse;
import com.keyloop.unifieddocumentviewer.exception.AuditLogNotFoundException;
import com.keyloop.unifieddocumentviewer.exception.InvalidRequestIdException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditLogQueryServiceTest {

	@TempDir
	private Path logDirectory;

	@Test
	void returnsMatchingRecordsFromMultipleFilesInChronologicalOrder() throws Exception {
		write("unifieddocumentviewer-audit-" + LocalDate.now() + ".log",
				json("req-002", "2026-08-12T13:30:20Z", "SERVICE"),
				"{invalid-json",
				json("req-001", "2026-08-12T13:30:10Z", "CONTROLLER"),
				"{partial");
		write("unifieddocumentviewer-audit-" + LocalDate.now().minusDays(1) + ".log",
				json("req-003", "2026-08-11T13:30:12Z", "SERVICE"),
				json("req-002", "2026-08-11T13:30:11Z", "CONTROLLER"));

		AuditLogLookupResponse response = service(2).findByRequestId("req-002");

		assertEquals("req-002", response.requestId());
		assertEquals(86409000L, response.durationMs());
		assertEquals(2, response.records().size());
		assertEquals("CONTROLLER", response.records().get(0).get("layer"));
		assertEquals("SERVICE", response.records().get(1).get("layer"));
	}

	@Test
	void throwsNotFoundWhenRequestIdDoesNotExistAndMissingFilesAreIgnored() {
		assertThrows(AuditLogNotFoundException.class, () -> service(7).findByRequestId("req-999"));
	}

	@Test
	void rejectsInvalidRequestIds() {
		assertThrows(InvalidRequestIdException.class, () -> service(7).findByRequestId("../secret"));
		assertThrows(InvalidRequestIdException.class, () -> service(7).findByRequestId(""));
	}

	private AuditLogQueryService service(int maxDays) {
		AuditLogQueryProperties properties = new AuditLogQueryProperties(
				logDirectory.toString(), "unifieddocumentviewer", maxDays);
		return new AuditLogQueryService(properties, new AuditLogFileReader(new ObjectMapper()));
	}

	private void write(String fileName, String... lines) throws Exception {
		Path auditDirectory = logDirectory.resolve("audit");
		Files.createDirectories(auditDirectory);
		Files.write(auditDirectory.resolve(fileName), String.join(System.lineSeparator(), lines).getBytes());
	}

	private String json(String requestId, String startTime, String layer) {
		return """
				{"timestamp":"%s","applicationName":"unifieddocumentviewer","requestId":"%s","userId":"user-001","tenantId":"tenant-001","layer":"%s","className":"DocumentController","methodName":"searchDocuments","status":"SUCCESS","startTime":"%s","endTime":"%s","durationMs":1}
				""".formatted(startTime, requestId, layer, startTime, startTime).trim();
	}
}
