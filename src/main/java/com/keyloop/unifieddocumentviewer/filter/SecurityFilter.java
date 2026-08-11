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
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String userId = extractUserId(request.getHeader(HttpHeaders.AUTHORIZATION));
		if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(userId, null, List.of()));
		}

		filterChain.doFilter(request, response);
	}

	private String extractUserId(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}

		String[] tokenParts = authorizationHeader.substring(BEARER_PREFIX.length()).split("\\.");
		if (tokenParts.length < 2) {
			return null;
		}

		try {
			String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
			Matcher matcher = USER_ID_PATTERN.matcher(payload);
			return matcher.find() ? matcher.group(1) : null;
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}
}
