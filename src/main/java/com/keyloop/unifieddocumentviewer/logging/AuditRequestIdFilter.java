package com.keyloop.unifieddocumentviewer.logging;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class AuditRequestIdFilter extends Filter<ILoggingEvent> {

	@Override
	public FilterReply decide(ILoggingEvent event) {
		if (event == null) {
			return FilterReply.DENY;
		}
		Map<String, String> mdc = event.getMDCPropertyMap();
		String requestId = mdc == null ? null : mdc.get(LoggingMdc.REQUEST_ID);
		return requestId != null && !requestId.isBlank() ? FilterReply.ACCEPT : FilterReply.DENY;
	}
}
