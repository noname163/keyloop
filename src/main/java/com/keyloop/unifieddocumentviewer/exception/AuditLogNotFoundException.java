package com.keyloop.unifieddocumentviewer.exception;

public class AuditLogNotFoundException extends RuntimeException {

	public AuditLogNotFoundException(String requestId) {
		super("No audit records were found for the supplied requestId.");
	}
}
