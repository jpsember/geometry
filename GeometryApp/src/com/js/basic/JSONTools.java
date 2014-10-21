package com.js.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONTools {

	/**
	 * Get keys from JSONObject as an Iterable<String>
	 * 
	 * @param object
	 * @return
	 */
	public static Iterable<String> keys(JSONObject object) {
		return toList((Iterator<String>) object.keys());
	}

	/**
	 * @deprecated not used at present
	 */
	public static Iterable<String> keys(Map map) {
		return toList((Iterator<String>) map.keySet());
	}

	public static JSONObject parseMap(String source) throws JSONException {
		return (JSONObject) new JSONTokener(source).nextValue();
	}

	/**
	 * Construct a list from an iterator
	 */
	private static <T> List<T> toList(Iterator<T> iter) {
		List list = new ArrayList();
		while (iter.hasNext())
			list.add(iter.next());
		return list;
	}

}
