package io.kanouken.easyflow.exception;

public class WorkflowException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8008127565703130605L;

	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public WorkflowException(String message) {
		this.message = message;
	}

}
