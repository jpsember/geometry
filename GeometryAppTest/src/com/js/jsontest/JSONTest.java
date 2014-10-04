package com.js.jsontest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.js.basic.JSONTools;
import com.js.testUtils.*;
import static com.js.basic.JSONTools.*;

public class JSONTest extends MyTestCase {

	private static final String mapScript = swapQuotes("{'a':5,'b':null,'c':17}");

	/**
	 * Convert a map to a list, with each key=>value represented as consecutive
	 * elements; sorts keys into alphabetical order
	 * 
	 * Provided to represent maps in a canonical (consistent) order, since map
	 * keys are unordered
	 * 
	 */
	private static ArrayList mapToArray(Map<String, Object> map) {
		ArrayList list = new ArrayList();
		ArrayList<String> keys = new ArrayList(map.keySet());
		Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
		for (String key : keys) {
			list.add(key);
			list.add(map.get(key));
		}
		return list;
	}

	public void testDecodeMap() throws JSONException {
		JSONObject object = (JSONObject) new JSONTokener(mapScript).nextValue();
		assertTrue(object.has("a"));
		assertFalse(object.has("d"));
		assertTrue(object.has("b"));
		Object bValue = object.get("b");
		assertTrue(bValue != null);
		assertEquals(bValue, JSONObject.NULL);
	}

	private void examineMap(Map<String, Object> map) {
		assertTrue(map.containsKey("a"));
		assertFalse(map.containsKey("d"));
		assertTrue(map.containsKey("b"));
		Object bValue = map.get("b");
		assertEquals(bValue, JSONObject.NULL);
	}

	public void testParseJSONStringObject() {
		Map<String, Object> map = parseObject(mapScript);
		examineMap(map);
	}

	public void testParseJSONObject() throws JSONException {
		JSONObject object = (JSONObject) new JSONTokener(mapScript).nextValue();
		Map<String, Object> map = parseObject(object);
		examineMap(map);
	}

	public void testEncode() {
		Map<String, Object> map = parseObject(mapScript);
		ArrayList a1 = mapToArray(map);

		String jsonString = JSONTools.encode(map);
		Map<String, Object> map2 = parseObject(jsonString);
		ArrayList a2 = mapToArray(map2);
		assertEquals(a1, a2);
	}

}
