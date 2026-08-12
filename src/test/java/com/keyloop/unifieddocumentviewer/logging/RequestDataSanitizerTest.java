package com.keyloop.unifieddocumentviewer.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class RequestDataSanitizerTest {

	private final RequestDataSanitizer sanitizer = new RequestDataSanitizer();

	@Test
	void logsAllowedFieldsMasksVinAndExcludesSensitiveFields() {
		Map<String, Object> data = sanitizer.sanitize(LogLayer.CONTROLLER,
				new Object[] { new SearchDocumentRequest("1HGCM82633A004352", "SERVICE_REPORT", 0, "secret") });

		assertEquals("1HG***4352", data.get("vin"));
		assertEquals("SERVICE_REPORT", data.get("documentType"));
		assertEquals(0, data.get("page"));
		assertFalse(data.containsKey("password"));
	}

	@Test
	void unexpectedObjectTypesAreIgnored() {
		assertNull(sanitizer.sanitize(LogLayer.SERVICE, new Object[] { new Object() }));
	}

	record SearchDocumentRequest(String vin, String documentType, Integer page, String password) {
	}
}
