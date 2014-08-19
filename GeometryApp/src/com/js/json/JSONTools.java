package com.js.json;

public class JSONTools {

	public static final String JSON_MIME_TYPE = "application/json";

	/**
	 * Utility method that exchanges single and double quotes, to allow easier
	 * embedding of JSON strings within Java source.
	 * 
	 * @param s
	 * @return
	 */
	public static String swapQuotes(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\'':
				c = '"';
				break;
			case '"':
				c = '\'';
				break;
			}
			sb.append(c);
		}
		return sb.toString();
	}

}
