package com.keyloop.unifieddocumentviewer.logging;

import java.security.Principal;

public record AuthenticatedUser(String userId, String tenantId) implements Principal {

	@Override
	public String getName() {
		return userId;
	}
}
