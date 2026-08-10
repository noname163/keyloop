package com.keyloop.unifieddocumentviewer.service.impl;

import com.keyloop.unifieddocumentviewer.service.TokenGenerationService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("develop")
@Service
public class TokenGenerationServiceImpl implements TokenGenerationService {

	// TODO: Implement development-only token generation.
}
