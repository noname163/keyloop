package com.keyloop.unifieddocumentviewer.service.impl;

import com.keyloop.unifieddocumentviewer.entity.DocumentSearchAudit;
import com.keyloop.unifieddocumentviewer.service.AuditService;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

	@Override
	public void recordSearchAudit(DocumentSearchAudit audit) {
		// TODO: Implement audit persistence.
	}
}
