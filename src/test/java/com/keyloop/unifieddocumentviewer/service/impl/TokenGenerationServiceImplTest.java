package com.keyloop.unifieddocumentviewer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class TokenGenerationServiceImplTest {

	private final TokenGenerationServiceImpl service = new TokenGenerationServiceImpl();

	@Test
	void generateTokenCreatesJwtPayloadContainingUserId() {
		String token = service.generateToken(" user-123 ");

		String[] tokenParts = token.split("\\.", -1);
		assertEquals(3, tokenParts.length);
		assertEquals("", tokenParts[2]);

		String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
		assertTrue(payload.contains("\"userId\":\"user-123\""));
		assertTrue(payload.contains("\"iat\":"));
	}

	@Test
	void generateTokenRejectsBlankUserId() {
		assertThrows(IllegalArgumentException.class, () -> service.generateToken(" "));
	}
}
