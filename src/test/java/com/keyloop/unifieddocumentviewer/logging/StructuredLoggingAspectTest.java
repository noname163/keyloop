package com.keyloop.unifieddocumentviewer.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingAspectTest {

	private final StructuredLoggingAspect aspect = new StructuredLoggingAspect(new RequestDataSanitizer(),
			"unifieddocumentviewer", "test", "test-instance");

	@Test
	void controllerServiceAndRepositoryOperationsAreLogged(CapturedOutput output) throws Throwable {
		assertEquals("ok", aspect.logOperation(joinPoint("com.keyloop.unifieddocumentviewer.controller.DocumentController",
				DocumentController.class, "searchDocuments", "ok")));
		assertEquals("ok", aspect.logOperation(joinPoint("com.keyloop.unifieddocumentviewer.service.impl.DocumentAggregationServiceImpl",
				DocumentAggregationServiceImpl.class, "searchDocumentsByVin", "ok")));
		assertEquals("ok", aspect.logOperation(joinPoint("com.keyloop.unifieddocumentviewer.repository.VehicleRepository",
				VehicleRepository.class, "findAllByTenantId", "ok")));

		String logs = output.getOut() + output.getErr();
		assertContains(logs, "controller operation success");
		assertContains(logs, "service operation success");
		assertContains(logs, "repository operation success");
		assertFalse(logs.contains("layer=CONTROLLER"));
		assertFalse(logs.contains("status=SUCCESS"));
		assertFalse(logs.contains("durationMs="));
		assertFalse(logs.contains("controller operation success {"));
	}

	@Test
	void failedOperationIsLoggedAndOriginalExceptionPropagates(CapturedOutput output) throws Throwable {
		IllegalStateException failure = new IllegalStateException("boom");
		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> aspect.logOperation(joinPoint("com.keyloop.unifieddocumentviewer.service.impl.DocumentAggregationServiceImpl",
						DocumentAggregationServiceImpl.class, "searchDocumentsByVin", failure)));

		assertSame(failure, thrown);
		String logs = output.getOut() + output.getErr();
		assertContains(logs, "service operation failed");
		assertContains(logs, "java.lang.IllegalStateException: boom");
		assertFalse(logs.contains("status=FAILED"));
		assertFalse(logs.contains("exceptionType=java.lang.IllegalStateException"));
	}

	@Test
	void structuredFieldsAreArgumentsAndMessageIsHumanReadableOnly() throws Throwable {
		Logger logger = (Logger) LoggerFactory.getLogger(StructuredLoggingAspect.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			assertEquals("ok", aspect.logOperation(joinPoint("com.keyloop.unifieddocumentviewer.controller.DocumentController",
					DocumentController.class, "searchDocuments", "ok")));
		}
		finally {
			logger.detachAppender(appender);
		}

		ILoggingEvent event = appender.list.get(appender.list.size() - 1);
		assertEquals(Level.INFO, event.getLevel());
		assertEquals("controller operation success", event.getFormattedMessage());
		assertStructuredFields(event,
				"layer", "className", "methodName", "status", "startTime", "endTime", "durationMs", "requestData");
	}

	private ProceedingJoinPoint joinPoint(String declaringTypeName, Class<?> declaringType, String methodName,
			Object resultOrException) throws Throwable {
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		Signature signature = mock(Signature.class);
		when(signature.getDeclaringTypeName()).thenReturn(declaringTypeName);
		when(signature.getDeclaringType()).thenReturn((Class) declaringType);
		when(signature.getName()).thenReturn(methodName);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[] { "1HGCM82633A004352" });
		if (resultOrException instanceof Throwable throwable) {
			when(joinPoint.proceed()).thenThrow(throwable);
		}
		else {
			when(joinPoint.proceed()).thenReturn(resultOrException);
		}
		return joinPoint;
	}

	private void assertContains(String value, String expected) {
		if (!value.contains(expected)) {
			throw new AssertionError("Expected logs to contain " + expected + " but were:\n" + value);
		}
	}

	private void assertStructuredFields(ILoggingEvent event, String... expectedFieldNames) {
		if (event.getMarkerList() == null || event.getMarkerList().isEmpty()) {
			throw new AssertionError("Expected structured log markers");
		}
		String fields = event.getMarkerList().toString();
		List.of(expectedFieldNames).forEach(field -> assertContains(fields, field + "="));
	}

	private static final class DocumentController {
	}

	private static final class DocumentAggregationServiceImpl {
	}

	private interface VehicleRepository {
	}
}
