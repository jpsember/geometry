package com.js.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import static com.js.basic.Tools.*;

public class JSONTools {

	public static class JSONError extends RuntimeException {
		public JSONError(JSONException e) {
			super(e);
		}

		public JSONError(String detailMessage) {
			super(detailMessage);
		}

		public static void raise(JSONException e) {
			throw new JSONError(e);
		}

		public static void raise(String detailMessage) {
			throw new JSONError(detailMessage);
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
	 * Parse a JSON value
	 * 
	 * @param source
	 *            a JSONObject, JSONArray, or string representing some other
	 *            JSON value
	 * @return an appropriate value: Map, ArrayList, Boolean, etc
	 */
	public static Object parseValue(Object source) {
		try {
			JSONObject object;
			if (source instanceof JSONObject) {
				return parseJSONObject((JSONObject) source);
			} else if (source instanceof JSONArray) {
				return parseJSONArray((JSONArray) source);
			} else {
				object = (JSONObject) new JSONTokener((String) source)
						.nextValue();
				return parseJSONObject(object);
			}
		} catch (JSONException e) {
			warning("failed to parse: " + source);
			throw new JSONError(e);
		}
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
			warning("failed to parse: " + source);
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
				Object object = jsonObject.get(key);
				map.put(key, object);
			}
			return map;
		} catch (JSONException e) {
			throw new JSONError(e);
		}
	}

	/**
	 * Convert a JSONArray to an ArrayList
	 * 
	 * @param source
	 * @return
	 */
	private static ArrayList parseJSONArray(JSONArray source) {
		try {
			ArrayList array = new ArrayList();
			for (int i = 0; i < source.length(); i++) {
				array.add(source.get(i));
			}
			return array;
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
