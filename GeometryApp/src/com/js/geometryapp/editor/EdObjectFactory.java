package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.geometry.Point;

public abstract class EdObjectFactory {

	@Override
	public String toString() {
		return "EdObjectFactory '" + getTag() + "'";
	}

	public EdObjectFactory(String tag) {
		mTag = tag;
	}

	/**
	 * Construct an event handler for editor operations with these objects
	 */
	public abstract EditorEventListener buildEditorOperation(Editor editor,
			int slot);

	/**
	 * Get name of this object. This is an identifier that is written to text
	 * files to identify this object.
	 */
	public String getTag() {
		return mTag;
	}

	/**
	 * Construct an EditObj of this type. Used when user wants to add a new
	 * object in the editor.
	 * 
	 * @return EditObj
	 */
	public abstract EdObject construct();

	/**
	 * Parse EdObject from a JSON object
	 * 
	 * @throws JSONException
	 */
	public EdObject parse(JSONObject map) throws JSONException {
		EdObject obj = construct();
		parsePoints(obj, map);
		return obj;
	}

	/**
	 * Construct Map from EdObject. Default implementation construct a partial
	 * map contiaining only "type" and "points" key/value pairs
	 */
	public Map write(EdObject obj) {
		Map map = new HashMap();
		map.put("type", getTag());
		ArrayList<Float> f = new ArrayList();
		for (int i = 0; i < 2; i++) {
			Point pt = obj.getPoint(i);
			f.add(pt.x);
			f.add(pt.y);
		}
		map.put("points", f);
		return map;
	}

	public void parsePoints(EdObject destinationObject, JSONObject map)
			throws JSONException {
		JSONArray coordinates = map.getJSONArray("points");
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
