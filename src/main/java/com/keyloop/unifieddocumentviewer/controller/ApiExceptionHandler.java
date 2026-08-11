package com.keyloop.unifieddocumentviewer.controller;

import com.keyloop.unifieddocumentviewer.dto.response.ErrorResponse;
import com.keyloop.unifieddocumentviewer.exception.InvalidVinException;
import com.keyloop.unifieddocumentviewer.exception.UpstreamDependencyException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidVinException.class)
	ResponseEntity<ErrorResponse> handleInvalidVin(InvalidVinException exception) {
		return ResponseEntity.badRequest().body(new ErrorResponse(
				"INVALID_VIN",
				exception.getMessage()));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
		return ResponseEntity.badRequest().body(new ErrorResponse(
				"INVALID_VIN",
				"The supplied VIN is invalid."));
	}

	@ExceptionHandler(UpstreamDependencyException.class)
	ResponseEntity<ErrorResponse> handleUpstreamDependency(UpstreamDependencyException exception) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(
				"UPSTREAM_DEPENDENCY_FAILURE",
				exception.getMessage()));
	}
}
