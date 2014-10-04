package com.js.jsontest;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.js.testUtils.*;
import static com.js.basic.JSONTools.*;

public class JSONTest extends MyTestCase {

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

}
