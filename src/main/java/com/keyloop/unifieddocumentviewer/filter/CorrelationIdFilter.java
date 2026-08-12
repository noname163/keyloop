package com.keyloop.unifieddocumentviewer.filter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import com.keyloop.unifieddocumentviewer.logging.LoggingMdc;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-ID";

	private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
		response.setHeader(REQUEST_ID_HEADER, requestId);
		MDC.put(LoggingMdc.REQUEST_ID, requestId);
		MDC.put(LoggingMdc.API_NAME, request.getMethod() + " " + request.getRequestURI());
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			MDC.clear();
		}
	}

	private String requestId(String headerValue) {
		if (headerValue != null) {
			String trimmed = headerValue.trim();
			if (REQUEST_ID_PATTERN.matcher(trimmed).matches()) {
				return trimmed;
			}
		}
		return UUID.randomUUID().toString();
	}
}
