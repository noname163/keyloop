package com.keyloop.unifieddocumentviewer.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.keyloop.unifieddocumentviewer.service.TokenGenerationService;
import org.springframework.stereotype.Service;

@Service
public class TokenGenerationServiceImpl implements TokenGenerationService {

	@Override
	public String generateToken(String userId) {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("userId is required.");
		}

		String normalizedUserId = userId.trim();
		String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
		String payload = "{\"userId\":\"" + escapeJson(normalizedUserId) + "\",\"iat\":" + Instant.now().getEpochSecond()
				+ "}";
		return base64Url(header) + "." + base64Url(payload) + ".";
	}

	private String base64Url(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
