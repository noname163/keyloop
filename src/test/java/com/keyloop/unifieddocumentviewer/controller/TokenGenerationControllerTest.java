package com.keyloop.unifieddocumentviewer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.keyloop.unifieddocumentviewer.dto.TokenGenerationRequest;
import com.keyloop.unifieddocumentviewer.dto.response.TokenGenerationResponse;
import com.keyloop.unifieddocumentviewer.service.TokenGenerationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenGenerationControllerTest {

	@Mock
	private TokenGenerationService tokenGenerationService;

	@Test
	void generateTokenReturnsGeneratedToken() {
		when(tokenGenerationService.generateToken("user-123")).thenReturn("generated-token");
		TokenGenerationController controller = new TokenGenerationController(tokenGenerationService);

		TokenGenerationResponse response = controller.generateToken(new TokenGenerationRequest("user-123"));

		assertEquals("generated-token", response.token());
	}
}
