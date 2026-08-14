package com.keyloop.unifieddocumentviewer.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ContextPropagatingExecutorServiceTest {

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void requestIdSurvivesThreadSwitching() throws Exception {
		ExecutorService executor = new ContextPropagatingExecutorService(Executors.newSingleThreadExecutor());
		MDC.put(LoggingMdc.REQUEST_ID, "request-123");

		try {
			assertEquals("request-123", executor.submit(() -> MDC.get(LoggingMdc.REQUEST_ID)).get());
			assertEquals("request-123", MDC.get(LoggingMdc.REQUEST_ID));
		}
		finally {
			executor.shutdownNow();
		}
	}
}
