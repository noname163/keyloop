package com.keyloop.unifieddocumentviewer.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;

class AuditRequestIdFilterTest {

	private final AuditRequestIdFilter filter = new AuditRequestIdFilter();

	@Test
	void acceptsEventWithRequestId() {
		assertEquals(FilterReply.ACCEPT, filter.decide(event(Map.of(LoggingMdc.REQUEST_ID, "req-123"))));
	}

	@Test
	void deniesEventWithoutRequestId() {
		assertEquals(FilterReply.DENY, filter.decide(event(Map.of())));
	}

	@Test
	void deniesEventWithBlankRequestId() {
		assertEquals(FilterReply.DENY, filter.decide(event(Map.of(LoggingMdc.REQUEST_ID, "   "))));
	}

	private ILoggingEvent event(Map<String, String> mdc) {
		ILoggingEvent event = mock(ILoggingEvent.class);
		when(event.getMDCPropertyMap()).thenReturn(mdc);
		return event;
	}
}
