package com.keyloop.unifieddocumentviewer.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.keyloop.unifieddocumentviewer.logging.ContextPropagatingExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentAggregationConfig {

	@Bean(destroyMethod = "shutdown")
	public ExecutorService documentAggregationExecutor() {
		int threadCount = Math.max(4, Runtime.getRuntime().availableProcessors());
		return new ContextPropagatingExecutorService(Executors.newFixedThreadPool(threadCount));
	}
}
