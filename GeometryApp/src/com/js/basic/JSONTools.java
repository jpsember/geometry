package com.js.basic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONTools {

	public static class JSONError extends RuntimeException {
		public JSONError(JSONException e) {
			super(e);
		}

		public static void raise(JSONException e) {
			throw new JSONError(e);
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
	 * Parse a json object, return as map
	 * 
	 * @param source
	 *            either a JSONObject, or a String which is assumed to be a JSON
	 *            string
	 * @return map of String -> Object
	 * @throws JSONError
	 */
	public static Map<String, Object> parseObject(Object source) {

		try {
			JSONObject object;
			if (source instanceof JSONObject) {
				object = (JSONObject) source;
			} else {
				object = (JSONObject) new JSONTokener((String) source)
						.nextValue();
			}
			return parseJSONObject(object);
		} catch (JSONException e) {
			throw new JSONError(e);
		}
	}

	/**
	 * Convert a JSONObject to a Map
	 * 
	 * @return map of String -> Object
	 * @throws JSONError
	 */
	private static Map<String, Object> parseJSONObject(JSONObject jsonObject) {
		try {
			Map<String, Object> map = new HashMap();

			Iterator<String> iter = jsonObject.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				map.put(key, jsonObject.get(key));
			}
			return map;
		} catch (JSONException e) {
			throw new JSONError(e);
		}
	}

	/**
	 * Encode a map into a JSON string
	 */
	public static String encode(Map map) {
		JSONObject obj = new JSONObject(map);
		return obj.toString();
	}

}
