package com.keyloop.unifieddocumentviewer.logging;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class ContextPropagatingExecutorService extends AbstractExecutorService {

	private final ExecutorService delegate;

	public ContextPropagatingExecutorService(ExecutorService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return delegate.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return delegate.awaitTermination(timeout, unit);
	}

	@Override
	public void execute(Runnable command) {
		delegate.execute(wrap(command));
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return delegate.submit(wrap(task));
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return delegate.submit(wrap(task), result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return delegate.submit(wrap(task));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return delegate.invokeAll(tasks.stream().map(this::wrap).toList());
	}

	private Runnable wrap(Runnable task) {
		Map<String, String> callerMdc = MDC.getCopyOfContextMap();
		SecurityContext callerSecurityContext = SecurityContextHolder.getContext();
		return () -> {
			Map<String, String> previousMdc = MDC.getCopyOfContextMap();
			SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
			try {
				if (callerMdc == null) {
					MDC.clear();
				}
				else {
					MDC.setContextMap(callerMdc);
				}
				SecurityContextHolder.setContext(callerSecurityContext);
				task.run();
			}
			finally {
				if (previousMdc == null) {
					MDC.clear();
				}
				else {
					MDC.setContextMap(previousMdc);
				}
				SecurityContextHolder.setContext(previousSecurityContext);
			}
		};
	}

	private <T> Callable<T> wrap(Callable<T> task) {
		Map<String, String> callerMdc = MDC.getCopyOfContextMap();
		SecurityContext callerSecurityContext = SecurityContextHolder.getContext();
		return () -> {
			Map<String, String> previousMdc = MDC.getCopyOfContextMap();
			SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
			try {
				if (callerMdc == null) {
					MDC.clear();
				}
				else {
					MDC.setContextMap(callerMdc);
				}
				SecurityContextHolder.setContext(callerSecurityContext);
				return task.call();
			}
			finally {
				if (previousMdc == null) {
					MDC.clear();
				}
				else {
					MDC.setContextMap(previousMdc);
				}
				SecurityContextHolder.setContext(previousSecurityContext);
			}
		};
	}
}
