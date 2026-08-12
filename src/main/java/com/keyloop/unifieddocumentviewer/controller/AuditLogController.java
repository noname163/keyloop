package com.keyloop.unifieddocumentviewer.controller;

import com.keyloop.unifieddocumentviewer.dto.response.AuditLogLookupResponse;
import com.keyloop.unifieddocumentviewer.service.audit.AuditLogQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditLogController {

	private final AuditLogQueryService auditLogQueryService;

	public AuditLogController(AuditLogQueryService auditLogQueryService) {
		this.auditLogQueryService = auditLogQueryService;
	}

	@GetMapping("/{requestId}")
	public AuditLogLookupResponse findByRequestId(@PathVariable String requestId) {
		return auditLogQueryService.findByRequestId(requestId);
	}
}
