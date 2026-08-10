package com.keyloop.unifieddocumentviewer.service;

import com.keyloop.unifieddocumentviewer.entity.DocumentSearchAudit;

public interface AuditService {

	void recordSearchAudit(DocumentSearchAudit audit);
}
