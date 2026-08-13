package com.keyloop.unifieddocumentviewer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.keyloop.unifieddocumentviewer.constants.SourceStatus;
import com.keyloop.unifieddocumentviewer.dto.response.DocumentSearchResponse;
import com.keyloop.unifieddocumentviewer.entity.UnifiedDocument;
import com.keyloop.unifieddocumentviewer.service.DocumentAggregationService;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

	private MockMvc mockMvc;

	@Mock
	private DocumentAggregationService documentAggregationService;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(documentAggregationService))
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@Test
	void searchDocumentsReturnsDocumentsWhenVinIsValid() throws Exception {
		String vin = "1HGCM82633A004352";
		when(documentAggregationService.searchDocumentsByVin(vin)).thenReturn(new DocumentSearchResponse(vin, false,
				Map.of("sales", SourceStatus.SUCCESS, "service", SourceStatus.SUCCESS),
				List.of(new UnifiedDocument("DOC-1", "Title", "TYPE", "SALES", Instant.parse("2026-07-01T09:00:00Z")))));

		mockMvc.perform(get("/api/v1/vehicles/{vin}/documents", vin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vin").value(vin))
				.andExpect(jsonPath("$.documents[0].id").value("DOC-1"));
	}

	@Test
	void searchDocumentsUsesAnnotationVinValidation() throws Exception {
		Method method = DocumentController.class.getMethod("searchDocuments", String.class);
		Pattern pattern = method.getParameters()[0].getAnnotation(Pattern.class);

		assertNotNull(pattern);
		assertEquals("^[A-HJ-NPR-Z0-9a-hj-npr-z0-9]{17}$", pattern.regexp());
		assertEquals("The supplied VIN is invalid.", pattern.message());
		verifyNoInteractions(documentAggregationService);
	}
}
