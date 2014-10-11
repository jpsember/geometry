package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.geometry.Point;

public abstract class EdObjectFactory {

	static final String JSON_KEY_TYPE = "t";
	static final String JSON_KEY_VERTICES = "p";

	@Override
	public String toString() {
		return "EdObjectFactory '" + getTag() + "'";
	}

	public EdObjectFactory(String tag) {
		mTag = tag;
	}

	/**
	 * Get name of this object. This is an identifier that is written to text
	 * files to identify this object.
	 */
	public String getTag() {
		return mTag;
	}

	/**
	 * Construct an object of this type
	 */
	public abstract <T extends EdObject> T construct();

	public abstract int minimumPoints();

	/**
	 * Parse EdObject from a JSON object
	 * 
	 * @throws JSONException
	 */
	public <T extends EdObject> T parse(JSONObject map) throws JSONException {
		T obj = construct();
		parsePoints(obj, map);
		return obj;
	}

	/**
	 * Construct Map from EdObject. Default implementation construct a partial
	 * map contiaining only JSON_KEY_TYPE and JSON_KEY_VERTICES key/value pairs
	 */
	public Map write(EdObject obj) {
		Map map = new HashMap();
		map.put(JSON_KEY_TYPE, getTag());
		ArrayList<Integer> f = new ArrayList();
		for (int i = 0; i < obj.nPoints(); i++) {
			Point pt = obj.getPoint(i);
			f.add((int) pt.x);
			f.add((int) pt.y);
		}
		map.put(JSON_KEY_VERTICES, f);
		return map;
	}

	public void parsePoints(EdObject destinationObject, JSONObject map)
			throws JSONException {
		JSONArray coordinates = map.getJSONArray(JSON_KEY_VERTICES);
		if (coordinates.length() % 2 != 0)
			throw new IllegalArgumentException(
					"unexpected number of coordinates");
		int i = 0;
		while (i < coordinates.length()) {
			float x = coordinates.getLong(i);
			float y = coordinates.getLong(i + 1);
			destinationObject.addPoint(new Point(x, y));
			i += 2;
		}
	}

	private String mTag;
}
