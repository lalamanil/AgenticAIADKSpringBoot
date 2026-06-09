package com.agenticai.adk.app.exception;
/** @author lalamanil **/
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.agenticai.adk.app.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(value = { MethodArgumentNotValidException.class })
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, String> validationErrors = new HashMap<String, String>();
		ex.getBindingResult().getFieldErrors().forEach(error -> {
			validationErrors.put(error.getField(), error.getDefaultMessage());
		});
		ErrorResponse errorResponse = new ErrorResponse("VALIDATION_FAILED", validationErrors.toString());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	@ExceptionHandler(value = { AgentApplicationException.class })
	public ResponseEntity<ErrorResponse> handleApplicationRunTimeExceptions(AgentApplicationException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
	}

}
