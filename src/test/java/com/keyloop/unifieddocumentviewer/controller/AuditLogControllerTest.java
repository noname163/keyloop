package com.keyloop.unifieddocumentviewer.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.keyloop.unifieddocumentviewer.dto.response.AuditLogLookupResponse;
import com.keyloop.unifieddocumentviewer.exception.AuditLogNotFoundException;
import com.keyloop.unifieddocumentviewer.exception.InvalidRequestIdException;
import com.keyloop.unifieddocumentviewer.service.audit.AuditLogQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

	private MockMvc mockMvc;

	@Mock
	private AuditLogQueryService auditLogQueryService;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new AuditLogController(auditLogQueryService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void findByRequestIdReturnsMatchingRecords() throws Exception {
		String requestId = "req-002";
		when(auditLogQueryService.findByRequestId(requestId)).thenReturn(new AuditLogLookupResponse(requestId,
				List.of(Map.of("requestId", requestId, "layer", "CONTROLLER"))));

		mockMvc.perform(get("/api/v1/audits/{requestId}", requestId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requestId").value(requestId))
				.andExpect(jsonPath("$.records[0].layer").value("CONTROLLER"));
	}

	@Test
	void findByRequestIdReturns404ForNoMatch() throws Exception {
		when(auditLogQueryService.findByRequestId("req-404")).thenThrow(new AuditLogNotFoundException("req-404"));

		mockMvc.perform(get("/api/v1/audits/{requestId}", "req-404"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("AUDIT_LOG_NOT_FOUND"));
	}

	@Test
	void findByRequestIdReturns400ForInvalidRequestId() throws Exception {
		when(auditLogQueryService.findByRequestId("..secret")).thenThrow(new InvalidRequestIdException());

		mockMvc.perform(get("/api/v1/audits/{requestId}", "..secret"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_ID"));
	}
}
