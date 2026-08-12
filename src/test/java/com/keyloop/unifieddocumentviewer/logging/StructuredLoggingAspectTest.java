package com.keyloop.unifieddocumentviewer.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
		assertContains(logs, "layer=CONTROLLER");
		assertContains(logs, "layer=SERVICE");
		assertContains(logs, "layer=REPOSITORY");
		assertContains(logs, "status=SUCCESS");
		assertContains(logs, "startTime=");
		assertContains(logs, "endTime=");
		assertContains(logs, "durationMs=");
	}

	@Test
	void failedOperationIsLoggedAndOriginalExceptionPropagates(CapturedOutput output) throws Throwable {
		IllegalStateException failure = new IllegalStateException("boom");
		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> aspect.logOperation(joinPoint("com.keyloop.unifieddocumentviewer.service.impl.DocumentAggregationServiceImpl",
						DocumentAggregationServiceImpl.class, "searchDocumentsByVin", failure)));

		assertSame(failure, thrown);
		String logs = output.getOut() + output.getErr();
		assertContains(logs, "status=FAILED");
		assertContains(logs, "exceptionType=java.lang.IllegalStateException");
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

	private static final class DocumentController {
	}

	private static final class DocumentAggregationServiceImpl {
	}

	private interface VehicleRepository {
	}
}
