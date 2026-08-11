package com.keyloop.unifieddocumentviewer.controller;

import com.keyloop.unifieddocumentviewer.dto.TokenGenerationRequest;
import com.keyloop.unifieddocumentviewer.dto.response.TokenGenerationResponse;
import com.keyloop.unifieddocumentviewer.service.TokenGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Token Generation")
@RestController
@RequestMapping("/api/develop/tokens")
public class TokenGenerationController {

	private final TokenGenerationService tokenGenerationService;

	public TokenGenerationController(TokenGenerationService tokenGenerationService) {
		this.tokenGenerationService = tokenGenerationService;
	}

	@Operation(summary = "Generate a development bearer token")
	@PostMapping
	public TokenGenerationResponse generateToken(@RequestBody TokenGenerationRequest request) {
		return new TokenGenerationResponse(tokenGenerationService.generateToken(request.userId()));
	}
}
