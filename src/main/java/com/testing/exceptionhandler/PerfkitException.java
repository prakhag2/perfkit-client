package com.testing.exceptionhandler;

public class PerfkitException extends RuntimeException{
	private static final long serialVersionUID = 1L;
	public PerfkitException(Exception e) {
		super(e);
	}
}
