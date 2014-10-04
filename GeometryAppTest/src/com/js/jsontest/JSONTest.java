package com.js.jsontest;

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

}
