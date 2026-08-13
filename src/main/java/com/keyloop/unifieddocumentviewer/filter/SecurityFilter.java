package com.keyloop.unifieddocumentviewer.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.keyloop.unifieddocumentviewer.logging.LoggingMdc;
import com.keyloop.unifieddocumentviewer.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;

@Component
public class SecurityFilter extends OncePerRequestFilter {

	private static final Pattern USER_ID_PATTERN = Pattern.compile("\"(?:sub|userId|user_id)\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern TENANT_ID_PATTERN = Pattern.compile("\"(?:tenantId|tenant_id|tid)\"\\s*:\\s*\"([^\"]+)\"");
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		AuthenticatedUser authenticatedUser = extractAuthenticatedUser(request.getHeader(HttpHeaders.AUTHORIZATION));
		if (authenticatedUser != null) {
			MDC.put(LoggingMdc.USER_ID, authenticatedUser.userId());
			if (authenticatedUser.tenantId() != null) {
				MDC.put(LoggingMdc.TENANT_ID, authenticatedUser.tenantId());
			}
			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of()));
			}
		}

		filterChain.doFilter(request, response);
	}

	private AuthenticatedUser extractAuthenticatedUser(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}

		String[] tokenParts = authorizationHeader.substring(BEARER_PREFIX.length()).split("\\.");
		if (tokenParts.length < 2) {
			return null;
		}

		try {
			String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
			String userId = extractClaim(payload, USER_ID_PATTERN);
			if (userId == null) {
				return null;
			}
			return new AuthenticatedUser(userId, extractClaim(payload, TENANT_ID_PATTERN));
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private String extractClaim(String payload, Pattern pattern) {
		Matcher matcher = pattern.matcher(payload);
		return matcher.find() ? matcher.group(1) : null;
	}
}
