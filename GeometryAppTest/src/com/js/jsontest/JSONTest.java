package com.js.jsontest;

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

	public void testDecodeMap() throws org.json.JSONException {
		String s = "{'a':5,'b':null,'c':17}";
		s = swapQuotes(s);
		JSONObject object = (JSONObject) new JSONTokener(s).nextValue();
		assertTrue(object.has("a"));
		assertFalse(object.has("d"));
		assertTrue(object.has("b"));
		Object bValue = object.get("b");
		assertTrue(bValue != null);
		assertEquals(bValue, JSONObject.NULL);
	}

}
