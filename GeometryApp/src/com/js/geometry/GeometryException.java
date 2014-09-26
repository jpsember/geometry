package com.js.geometry;

import static com.js.basic.Tools.*;


public class GeometryException extends RuntimeException {
	public GeometryException(String message) {
		super(message);
	}

	public static void raise(String message) {
		throw new GeometryException(message);
	}
}
