package com.js.json;

public class JSONException extends RuntimeException {

	public JSONException(Throwable e) {
		super(e);
	}

	public JSONException(String message) {
		super(message);
	}

	@Override
	public String toString() {
		return super.toString();
	}

}
