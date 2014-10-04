package com.js.jsontest;

import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.js.testUtils.*;
import static com.js.basic.JSONTools.*;

public class JSONTest extends MyTestCase {

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

	public void testParseMap() {
		String s = "{'a':5,'b':null,'c':17}";
		Map<String, Object> map = parseObject(s);
		assertTrue(map.containsKey("a"));
		assertFalse(map.containsKey("d"));
		assertTrue(map.containsKey("b"));
		Object bValue = map.get("b");
		assertEquals(bValue, JSONObject.NULL);
	}

}
