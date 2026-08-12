package com.keyloop.unifieddocumentviewer.logging;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import net.logstash.logback.argument.StructuredArguments;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class StructuredLoggingAspect {

	private static final Logger LOGGER = LoggerFactory.getLogger(StructuredLoggingAspect.class);

	private final RequestDataSanitizer requestDataSanitizer;
	private final String applicationName;
	private final String environment;
	private final String instanceId;

	public StructuredLoggingAspect(RequestDataSanitizer requestDataSanitizer,
			@Value("${spring.application.name:unified-document-viewer}") String applicationName,
			@Value("${APP_ENV:local}") String environment,
			@Value("${INSTANCE_ID:${HOSTNAME:local}}") String instanceId) {
		this.requestDataSanitizer = requestDataSanitizer;
		this.applicationName = applicationName;
		this.environment = environment;
		this.instanceId = instanceId;
	}

	@Around("("
			+ "within(com.keyloop.unifieddocumentviewer.controller..*)"
			+ " || within(com.keyloop.unifieddocumentviewer.service..*)"
			+ " || within(com.keyloop.unifieddocumentviewer.repository..*)"
			+ ")"
			+ " && !within(com.keyloop.unifieddocumentviewer.service.audit..*)")
	public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
		LogLayer layer = layer(joinPoint.getSignature().getDeclaringTypeName());
		Instant startTime = Instant.now();
		long startNanos = System.nanoTime();
		try {
			Object result = joinPoint.proceed();
			log(joinPoint, layer, LogStatus.SUCCESS, startTime, startNanos, null);
			return result;
		}
		catch (Throwable exception) {
			log(joinPoint, layer, LogStatus.FAILED, startTime, startNanos, exception);
			throw exception;
		}
	}

	private void log(ProceedingJoinPoint joinPoint, LogLayer layer, LogStatus status, Instant startTime, long startNanos,
			Throwable exception) {
		try {
			Instant endTime = Instant.now();
			long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
			Map<String, Object> fields = new LinkedHashMap<>();
			fields.put("timestamp", endTime.toString());
			fields.put("applicationName", applicationName);
			fields.put("environment", environment);
			fields.put("instanceId", instanceId);
			putIfPresent(fields, "requestId", MDC.get(LoggingMdc.REQUEST_ID));
			putIfPresent(fields, "traceId", MDC.get(LoggingMdc.TRACE_ID));
			putIfPresent(fields, "spanId", MDC.get(LoggingMdc.SPAN_ID));
			putIfPresent(fields, "userId", MDC.get(LoggingMdc.USER_ID));
			putIfPresent(fields, "tenantId", MDC.get(LoggingMdc.TENANT_ID));
			putIfPresent(fields, "apiName", apiName());
			fields.put("layer", layer.name());
			fields.put("className", joinPoint.getSignature().getDeclaringType().getSimpleName());
			fields.put("methodName", joinPoint.getSignature().getName());
			fields.put("status", status.name());
			fields.put("startTime", startTime.toString());
			fields.put("endTime", endTime.toString());
			fields.put("durationMs", durationMs);
			fields.put("message", message(layer, status));
			Map<String, Object> requestData = requestDataSanitizer.sanitize(layer, joinPoint.getArgs());
			if (requestData != null && !requestData.isEmpty()) {
				fields.put("requestData", requestData);
			}
			if (exception != null) {
				fields.put("exceptionType", exception.getClass().getName());
				LOGGER.error("structured operation log {}", StructuredArguments.fields(fields), exception);
			}
			else {
				LOGGER.info("structured operation log {}", StructuredArguments.fields(fields));
			}
		}
		catch (RuntimeException loggingFailure) {
			LOGGER.warn("structured logging failed: {}", loggingFailure.getMessage());
		}
	}

	private LogLayer layer(String className) {
		if (className.contains(".controller.")) {
			return LogLayer.CONTROLLER;
		}
		if (className.contains(".repository.")) {
			return LogLayer.REPOSITORY;
		}
		return LogLayer.SERVICE;
	}

	private String apiName() {
		String apiName = MDC.get(LoggingMdc.API_NAME);
		return apiName != null ? apiName : null;
	}

	private String message(LogLayer layer, LogStatus status) {
		return layer.name().toLowerCase() + " operation " + status.name().toLowerCase();
	}

	private void putIfPresent(Map<String, Object> fields, String key, String value) {
		if (value != null && !value.isBlank()) {
			fields.put(key, value);
		}
	}
}
