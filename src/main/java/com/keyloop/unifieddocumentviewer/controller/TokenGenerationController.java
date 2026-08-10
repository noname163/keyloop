package com.keyloop.unifieddocumentviewer.controller;

import com.keyloop.unifieddocumentviewer.service.TokenGenerationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("develop")
@RestController
@RequestMapping("/api/develop/tokens")
public class TokenGenerationController {

	private final TokenGenerationService tokenGenerationService;

	public TokenGenerationController(TokenGenerationService tokenGenerationService) {
		this.tokenGenerationService = tokenGenerationService;
	}

	// TODO: Add development-only token generation API endpoint.
}
