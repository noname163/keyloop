package com.keyloop.unifieddocumentviewer.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityFilter extends OncePerRequestFilter {

	private static final Pattern USER_ID_PATTERN = Pattern.compile("\"(?:sub|userId|user_id)\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern TENANT_ID_PATTERN = Pattern.compile("\"(?:tenantId|tenant_id)\"\\s*:\\s*\"([^\"]+)\"");
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		TokenClaims tokenClaims = extractTokenClaims(request.getHeader(HttpHeaders.AUTHORIZATION));
		if (tokenClaims.userId() != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(tokenClaims.toPrincipal(), null, List.of()));
		}

		filterChain.doFilter(request, response);
	}

	private TokenClaims extractTokenClaims(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return TokenClaims.empty();
		}

		String[] tokenParts = authorizationHeader.substring(BEARER_PREFIX.length()).split("\\.");
		if (tokenParts.length < 2) {
			return TokenClaims.empty();
		}

		try {
			String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
			return new TokenClaims(extractClaim(USER_ID_PATTERN, payload), extractClaim(TENANT_ID_PATTERN, payload));
		}
		catch (IllegalArgumentException exception) {
			return TokenClaims.empty();
		}
	}

	private String extractClaim(Pattern pattern, String payload) {
		Matcher matcher = pattern.matcher(payload);
		return matcher.find() ? matcher.group(1) : null;
	}

	private record TokenClaims(String userId, String tenantId) {

		private static TokenClaims empty() {
			return new TokenClaims(null, null);
		}

		private com.keyloop.unifieddocumentviewer.security.AuthenticatedUser toPrincipal() {
			return new com.keyloop.unifieddocumentviewer.security.AuthenticatedUser(userId, tenantId);
		}
	}
}
