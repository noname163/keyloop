package com.keyloop.unifieddocumentviewer.service.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.PlaceholderResolutionException;

@Component
public class AuditLogQueryProperties {

	private final String logPath;
	private final String applicationName;
	private final int maxDays;

	@Autowired
	public AuditLogQueryProperties(Environment environment) {
		this(
				property(environment, "LOG_PATH", "logs"),
				property(environment, "spring.application.name", "unifieddocumentviewer"),
				integerProperty(environment, "audit.lookup.max-days", 7));
	}

	AuditLogQueryProperties(String logPath, String applicationName, int maxDays) {
		this.logPath = logPath;
		this.applicationName = applicationName;
		this.maxDays = Math.max(1, maxDays);
	}

	public String logPath() {
		return logPath;
	}

	public String applicationName() {
		return applicationName;
	}

	public int maxDays() {
		return maxDays;
	}

	private static String property(Environment environment, String key, String defaultValue) {
		String value;
		try {
			value = environment.getProperty(key);
		}
		catch (PlaceholderResolutionException exception) {
			return defaultValue;
		}
		return value == null || value.isBlank() || value.contains("${") ? defaultValue : value;
	}

	private static int integerProperty(Environment environment, String key, int defaultValue) {
		String value = property(environment, key, Integer.toString(defaultValue));
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException exception) {
			return defaultValue;
		}
	}
}
