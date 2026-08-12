package com.keyloop.unifieddocumentviewer.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.keyloop.unifieddocumentviewer.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityFilterTest {

	private final SecurityFilter securityFilter = new SecurityFilter();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterStoresUserIdFromJwtSubjectInSecurityContext() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithPayload("{\"sub\":\"user-123\"}"));

		securityFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertEquals("user-123", SecurityContextHolder.getContext().getAuthentication().getName());
	}

	@Test
	void doFilterStoresTenantIdFromJwtInSecurityContext() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION,
				"Bearer " + tokenWithPayload("{\"userId\":\"user-123\",\"tenantId\":\"tenant-123\"}"));

		securityFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext()
				.getAuthentication()
				.getPrincipal();
		assertEquals("tenant-123", principal.tenantId());
	}

	@Test
	void doFilterIgnoresMalformedBearerToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt");

		securityFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	private String tokenWithPayload(String payload) {
		return base64Url("{}") + "." + base64Url(payload) + ".signature";
	}

	private String base64Url(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}
}
