package com.agenticai.adk.app.exception;
/** @author lalamanil **/
public class AgentApplicationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private String message;
	private String errorCode;

	public AgentApplicationException(String errorCode, String message) {
		super(message);
		this.message = message;
		this.errorCode = errorCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

}
