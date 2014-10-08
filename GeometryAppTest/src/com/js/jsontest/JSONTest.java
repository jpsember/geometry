package com.js.jsontest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.js.testUtils.*;

public class JSONTest extends MyTestCase {

	/**
	 * Utility method that exchanges single and double quotes, to allow easier
	 * embedding of JSON strings within Java source.
	 */
	private static String swapQuotes(String s) {
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

	private static final String mapScript = swapQuotes("{'a':5,'b':null,'c':17}");

	public void testDecodeMap() throws JSONException {
		JSONObject object = (JSONObject) new JSONTokener(mapScript).nextValue();
		assertTrue(object.has("a"));
		assertFalse(object.has("d"));
		assertTrue(object.has("b"));
		Object bValue = object.get("b");
		assertTrue(bValue != null);
		assertEquals(bValue, JSONObject.NULL);
	}
}
