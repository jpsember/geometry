package com.js.geometryapp.editor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.geometry.Point;
import com.js.geometry.R;
import static com.js.basic.Tools.*;

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
	 * Get resource for icon to display within ButtonWidget corresponding to
	 * this type of object; default implementation returns -1
	 * 
	 * @return resource id, or -1 if none
	 */
	public int getIconResource() {
		if (true && DEBUG_ONLY_FEATURES) {
			warning("returning sample icon");
			return R.raw.sampleicon;
		}
		return -1;
	}

	/**
	 * Construct an object of this type
	 * 
	 * @param defaultLocation
	 *            if not null, default location; e.g. location where user first
	 *            touched to create object
	 */
	public abstract <T extends EdObject> T construct(Point defaultLocation);

	/**
	 * Parse EdObject from a JSON object
	 * 
	 * @throws JSONException
	 */
	public <T extends EdObject> T parse(JSONObject map) throws JSONException {
		T obj = construct(null);
		parsePoints(obj, map);
		return obj;
	}

	/**
	 * Construct Map from EdObject. Default implementation construct a partial
	 * map contiaining only JSON_KEY_TYPE and JSON_KEY_VERTICES key/value pairs
	 * 
	 * @throws JSONException
	 */
	public JSONObject write(EdObject obj) throws JSONException {
		JSONObject map = new JSONObject();
		map.put(JSON_KEY_TYPE, getTag());
		JSONArray vertexArray = new JSONArray();
		for (int i = 0; i < obj.nPoints(); i++) {
			Point pt = obj.getPoint(i);
			vertexArray.put((int) pt.x);
			vertexArray.put((int) pt.y);
		}
		map.put(JSON_KEY_VERTICES, vertexArray);
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
