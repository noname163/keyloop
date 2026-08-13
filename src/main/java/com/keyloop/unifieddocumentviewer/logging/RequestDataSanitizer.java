package com.keyloop.unifieddocumentviewer.logging;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestDataSanitizer {

	private static final Set<String> ALLOWED_FIELDS = Set.of("vin", "documentType", "page", "size", "sort", "userId");

	private static final Set<String> MASKED_FIELDS = Set.of("vin");

	private static final Set<String> SENSITIVE_TERMS = Set.of("authorization", "token", "password", "secret", "apikey",
			"apiKey", "credential", "content", "binary");

	public Map<String, Object> sanitize(LogLayer layer, Object[] args) {
		if (layer == LogLayer.REPOSITORY) {
			return Map.of("operation", "persistence");
		}
		Map<String, Object> data = new LinkedHashMap<>();
		if (layer == LogLayer.CONTROLLER) {
			addHttpRequestMetadata(data);
		}
		for (Object arg : args) {
			addSanitizedValue(data, arg);
		}
		return data.isEmpty() ? null : Map.copyOf(data);
	}

	private void addHttpRequestMetadata(Map<String, Object> data) {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			HttpServletRequest request = attributes.getRequest();
			data.put("httpMethod", request.getMethod());
			data.put("route", request.getRequestURI());
		}
	}

	private void addSanitizedValue(Map<String, Object> data, Object value) {
		if (value == null || value instanceof HttpServletRequest || value instanceof Pageable) {
			return;
		}
		if (isSimpleValue(value)) {
			addField(data, inferSimpleFieldName(value), value);
			return;
		}
		Class<?> type = value.getClass();
		if (!type.isRecord()) {
			return;
		}
		for (RecordComponent component : type.getRecordComponents()) {
			String fieldName = component.getName();
			if (!isAllowed(fieldName)) {
				continue;
			}
			try {
				addField(data, fieldName, component.getAccessor().invoke(value));
			}
			catch (ReflectiveOperationException | RuntimeException ignored) {
				data.remove(fieldName);
			}
		}
	}

	private void addField(Map<String, Object> data, String fieldName, Object value) {
		if (fieldName == null || value == null || !isAllowed(fieldName)) {
			return;
		}
		data.put(fieldName, MASKED_FIELDS.contains(fieldName) ? maskVin(String.valueOf(value)) : safeScalar(value));
	}

	private String inferSimpleFieldName(Object value) {
		String text = String.valueOf(value);
		return text.matches("[A-HJ-NPR-Za-hj-npr-z0-9]{17}") ? "vin" : null;
	}

	private boolean isAllowed(String fieldName) {
		if (fieldName == null || !ALLOWED_FIELDS.contains(fieldName)) {
			return false;
		}
		String normalized = fieldName.toLowerCase(Locale.ROOT);
		return SENSITIVE_TERMS.stream().noneMatch(normalized::contains);
	}

	private boolean isSimpleValue(Object value) {
		return value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof UUID;
	}

	private Object safeScalar(Object value) {
		return isSimpleValue(value) ? value : String.valueOf(value);
	}

	private String maskVin(String vin) {
		if (vin.length() <= 7) {
			return "***";
		}
		return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
	}
}
