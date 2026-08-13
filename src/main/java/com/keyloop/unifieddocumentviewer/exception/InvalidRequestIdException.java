package com.keyloop.unifieddocumentviewer.exception;

public class InvalidRequestIdException extends RuntimeException {

	public InvalidRequestIdException() {
		super("The supplied requestId is invalid.");
	}
}
