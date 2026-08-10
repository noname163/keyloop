package com.keyloop.unifieddocumentviewer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI unifiedDocumentViewerOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Unified Document Viewer API")
						.version("v1")
						.description("API documentation for the Unified Document Viewer."));
	}
}
