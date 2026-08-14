package com.keyloop.unifieddocumentviewer.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtils {

	private SecurityContextUtils() {
	}

	public static String currentTenantId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
			return null;
		}
		return authenticatedUser.tenantId();
	}

	public static String requiredTenantId() {
		String tenantId = currentTenantId();
		if (tenantId == null || tenantId.isBlank()) {
			throw new IllegalStateException("tenantId is required");
		}
		return tenantId;
	}
}
