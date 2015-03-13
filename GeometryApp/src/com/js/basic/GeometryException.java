package com.js.basic;

public class GeometryException extends RuntimeException {
	public GeometryException(String message) {
		super(message);
	}

	public static void raise(String message) {
		throw new GeometryException(message);
	}
}
