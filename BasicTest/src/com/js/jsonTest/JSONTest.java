package com.js.jsonTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.js.json.*;
import com.js.testUtils.*;

import static com.js.basic.Tools.*;
import static com.js.json.JSONTools.*;

public class JSONTest extends MyTest {

	private JSONParser json(String s) {
		json = new JSONParser(s);
		return json;
	}

	private JSONParser json;

	public void testNumbers() {
		String script[] = { "0", "1", "-123.52e20", "-123.52e-20", "0.5" };
		for (int i = 0; i < script.length; i++) {
			String s = script[i];
			json(s);

			double d = json.nextDouble();
			assertFalse(json.hasNext());
			assertEquals(Double.parseDouble(s), d, 1e-10);
		}
	}

	public void testBadNumbers() {
		// final boolean db = true;
		if (db)
			pr("\n\n\n\n----------------------------------------------------------\ntestBadNumbers\n");
		String script[] = { "-", "00", "12.", ".42", "123ee", "123e-", };
		for (int i = 0; i < script.length; i++) {
			String s = script[i];
			if (db)
				pr("\n-----------------\n constructing json for '" + s + "'");
			try {
				json(s);
				fail("expected exception with '" + s + "'");
			} catch (JSONException e) {
			}
		}
		if (db)
			pr("\n\n\n");
	}

	private JSONEncoder newEnc() {
		enc = null;
		return enc();
	}

	private JSONEncoder enc() {
		if (enc == null)
			enc = new JSONEncoder();
		return enc;
	}

	private JSONEncoder enc;

	public void testStreamConstructor() throws UnsupportedEncodingException {
		String orig = "[0,1,2,3,\"hello\"]";
		InputStream stream = new ByteArrayInputStream(orig.getBytes("UTF-8"));
		json = new JSONParser(stream);
		Object a = json.next();
		assertTrue(a instanceof ArrayList);

		enc().encode(a);
		String s = enc.toString();
		assertStringsMatch(s, orig);
	}

	public void testArray() {
		String orig = "[0,1,2,3,\"hello\"]";

		json(orig);
		json.enterList();
		for (int i = 0; i < 4; i++) {
			assertTrue(json.hasNext());
			assertEquals(i, json.nextInt());
		}
		assertTrue(json.hasNext());
		assertStringsMatch("hello", json.nextString());
		assertFalse(json.hasNext());
		json.exit();
	}

	public void testReadMapAsSingleObject() {
		String s = "{'description':{'type':'text','hint':'enter something here'}}";
		s = swapQuotes(s);
		json(s);
		Map map = (Map) json.next();
		assertTrue(map.containsKey("description"));
	}

	public void testMap() {
		String orig = "{\"u\":14,\"m\":false,\"w\":null,\"k\":true}";
		json(orig);

		json.enterMap();
		Map m = new HashMap();
		for (int i = 0; i < 4; i++) {
			String key = json.nextKey();
			Object value = json.next();
			assertFalse(m.containsKey(key));
			m.put(key, value);

		}
		json.exit();

		assertStringsMatch(m.get("u"), "14.0");
		assertStringsMatch(m.get("m"), "false");
		assertTrue(m.get("w") == null);
		assertStringsMatch(m.get("k"), "true");
	}

	public void testEncodeMap() {
		JSONEncoder enc = new JSONEncoder();
		enc.enterMap();
		enc.encode("a");

		enc.enterList();
		enc.encode(12);
		enc.encode(17);
		enc.exit();

		enc.encode("b");
		enc.encode(true);
		enc.exit();
		String s = enc.toString();
		assertStringsMatch("{\"a\":[12,17],\"b\":true}", s);
	}

	public void testArrays() {
		int[] intArray = { 1, 2, 3, 4 };
		enc().encode(intArray);
		String s = enc.toString();
		assertStringsMatch(s, "[1,2,3,4]");
	}

	public void testString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			sb.append((char) i);
		}
		String originalString = sb.toString();
		enc().encode(originalString);
		String jsonString = enc.toString();
		json(jsonString);
		String decodedString = json.nextString();
		assertStringsMatch(decodedString, originalString);
	}

	public void testSymmetry() {
		String script[] = {//
				"0",//
				"1",//
				"-1.2352E20",//
				"-1.2352E-20",//
				"0.5",//
				"{'hey':42}", //
				"[1,2,3,4]",//
				"{'hey':42,'you':43}",//
				"{'hey':{'you':17},'array':[1,2,3,4]}",//
				"{'trailing number':5}",//
				"{  'favorite song': { '_skip_order': 3, 'type': 'text','hint': 'name of song','zminlines': 5 },'wow':12 } ",//
		};

		for (int i = 0; i < script.length; i++) {
			String s = swapQuotes(script[i]);
			if (db)
				pr("\n testing '" + s + "'");

			json(s);
			Object obj = json.next();
			assertFalse(json.hasNext());
			if (db)
				pr("  parsed " + obj + " (type = " + obj.getClass() + ")");

			newEnc().encode(obj);
			String s2 = enc.toString();
			if (db)
				pr(" encoded is " + s2);

			Object obj2 = json(s2).next();
			if (db)
				pr("parsed object: " + obj2);

			newEnc().encode(obj2);
			String s3 = enc.toString();
			assertStringsMatch(s2, s3);
		}
	}

	public void testComments() {

		String script[] = {//
				"{'hey':// this is a comment\n  // this is also a comment\n 42}",//
				"{'hey':42}",//
				"[42,15// start of comment\n//Another comment immediately\n    //Another comment after spaces\n,16]",//
				"[42,15, 16]",//
				"[42,15// zzz\n//zzz\n//zzz\n,16]", "[42,15,16]",//
		};

		for (int i = 0; i < script.length; i += 2) {
			String s = swapQuotes(script[i + 0]);
			Object obj = json(s).next();

			String s2 = swapQuotes(script[i + 1]);
			Object obj2 = json(s2).next();

			newEnc().encode(obj);
			String enc1 = enc.toString();

			newEnc().encode(obj2);
			String enc2 = enc.toString();

			assertStringsMatch(enc1, enc2);
		}
	}

	public void testTrailingCommas() {

		String script[] = {//
		"{'hey':42,'you':'outthere',}",//
				"{'hey':42,'you':'outthere'}",//
				"[42,15,16,17,]",//
				"[42,15,16,17]",//
				"[42,15,16,17,null,]",//
				"[42,15,16,17,null]",//
		};

		for (int i = 0; i < script.length; i += 2) {
			String s = swapQuotes(script[i + 0]);
			Object obj = json(s).next();

			String s2 = swapQuotes(script[i + 1]);
			Object obj2 = json(s2).next();

			newEnc().encode(obj);
			String enc1 = enc.toString();

			newEnc().encode(obj2);
			String enc2 = enc.toString();

			assertStringsMatch(enc1, enc2);
		}
	}

	private static class OurClass implements IJSONEncoder {

		public static OurClass parse(JSONParser json) {
			json.enterList();
			String message = json.nextString();
			int number = json.nextInt();
			json.exit();
			return new OurClass(message, number);
		}

		public OurClass(String message, int number) {
			map.put("message", message);
			map.put("number", number);
			for (int i = 0; i < array.length; i++)
				array[i] = (number + i + 1) * (number + i + 1);
		}

		private int[] array = new int[3];
		private Map map = new HashMap();

		@Override
		public String toString() {
			return map.get("message") + "/" + map.get("number") + "/"
					+ Arrays.toString(array);
		}

		@Override
		public void encode(JSONEncoder encoder) {
			Object[] items = { map.get("message"), map.get("number") };
			encoder.encode(items);
		}
	}

	public void testInterface() {
		OurClass c = new OurClass("hello", 42);
		enc().encode(c);
		String s = enc().toString();
		OurClass c2 = OurClass.parse(new JSONParser(s));
		assertStringsMatch(c, c2);
	}

	public void testNullAsString() {
		json(swapQuotes("['hello',null,'there']"));
		json.enterList();
		assertStringsMatch("hello", json.nextString());
		assertNull(json.nextString());
		assertStringsMatch("there", json.nextString());
		assertFalse(json.hasNext());
	}

	public void testMapParsing() {
		Alpha a = Alpha.generateRandomObject(this.random());
		String s = JSONEncoder.toJSON(a);
		Alpha a2 = Alpha.parse(new JSONParser(s));
		String s2 = JSONEncoder.toJSON(a2);
		assertStringsMatch(s, s2);
	}

	public void testPeek() {
		int[] a = { 5, 12, -1, 17, 3, -1, 42, -1 };

		String s = "[5,12,null,17,3,null,42,null]";
		JSONParser p = new JSONParser(s);
		p.enterList();

		for (int i = 0; i < a.length; i++) {
			assertTrue(p.hasNext());
			Object q = p.peekNext();
			assertTrue((a[i] < 0) == (q == null));
			if (q == null) {
				assertTrue(p.nextIfNull());
			} else {
				assertFalse(p.nextIfNull());
				assertEquals(a[i], p.nextInt());
			}
		}
		assertFalse(p.hasNext());
		p.exit();
	}

	public void testPeek2() {
		String s = "{'a':5,'b':null,'c':17}";
		s = JSONTools.swapQuotes(s);

		JSONParser p = new JSONParser(s);
		int sum = 0;
		p.enterMap();
		while (p.hasNext()) {
			String key = p.nextKey();
			if (!p.nextIfNull()) {
				if (!key.equals("a")) {
					assertEquals("c", key);
				}
				int val = p.nextInt();
				sum += val;
			}
		}
		p.exit();
		assertEquals(5 + 17, sum);
	}

	// A class that represents an opaque data structure that supports JSON
	private static class Alpha implements IJSONEncoder {

		public static Alpha generateRandomObject(Random r) {
			Alpha a = new Alpha();
			for (int i = r.nextInt(6) + 2; i > 0; i--) {
				String key = "" + r.nextInt(10000);
				double value = r.nextDouble();
				a.map.put(key, value);
			}
			return a;
		}

		@Override
		public void encode(JSONEncoder encoder) {
			// TODO Auto-generated method stub
			encoder.enterMap();
			for (String s : map.keySet()) {
				encoder.encode(s);
				encoder.encode((Double) map.get(s));
			}
			encoder.exit();
		}

		public static Alpha parse(JSONParser json) {
			Alpha a = new Alpha();
			json.enterMap();
			while (json.hasNext()) {
				String key = json.nextKey();
				double d = json.nextDouble();
				a.map.put(key, d);
			}
			return a;
		}

		private Map<String, Double> map = new HashMap();
	}

}
