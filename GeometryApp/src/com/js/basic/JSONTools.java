package com.js.basic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import static com.js.basic.Tools.*;

public class JSONTools {

	public static class JSONError extends RuntimeException {
		public JSONError(JSONException e) {
			super(e);
		}
	}

	/**
	 * Utility method that exchanges single and double quotes, to allow easier
	 * embedding of JSON strings within Java source.
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

	/**
	 * Parse a JSONObject from a string, return as map
	 * 
	 * @param script
	 * @return map of String -> Object
	 * @throws JSONError
	 */
	public static Map<String, Object> parseObject(String script) {
		try {
			JSONObject object = (JSONObject) new JSONTokener(script)
					.nextValue();
			Map<String, Object> map = new HashMap();

			Iterator<String> iter = object.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				map.put(key, object.get(key));
			}
			return map;
		} catch (JSONException e) {
			throw new JSONError(e);
		}
	}
}
