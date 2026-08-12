package com.keyloop.unifieddocumentviewer.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.keyloop.unifieddocumentviewer.logging.LoggingMdc;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	void generatesRequestIdWhenHeaderIsAbsentAndClearsMdc() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(new MockHttpServletRequest("GET", "/documents"), response, new MockFilterChain());

		assertFalse(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER).isBlank());
		assertNull(MDC.get(LoggingMdc.REQUEST_ID));
	}

	@Test
	void reusesValidIncomingRequestId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/documents");
		request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "request-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertEquals("request-123", response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER));
	}
}
