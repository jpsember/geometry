package com.js.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.js.json.*;
import static com.js.basic.Tools.*;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 * Utility class to persist stateful elements for fragments
 * 
 * Instances of the class can be populated with certain types of stateful
 * objects. For example, one supported type are ListViews. The scroll position
 * of these views are saved and restored.
 * 
 * The types of objects that can be persisted:
 * 
 * [] ListView
 * 
 * [] ScrollView
 * 
 * [] Map (persisted via JSON)
 * 
 */
public class PersistentFragmentState {

	private static final String KEY_BUNDLE = "fragmentState";

	public void setLogging(boolean f) {
		mLogging = f;
	}

	private void log(Object message) {
		if (mLogging) {
			StringBuilder sb = new StringBuilder("---> ");
			sb.append(nameOf(this));
			sb.append(" : ");
			tab(sb, 30);
			sb.append(message);
			pr(sb);
		}
	}

	/**
	 * Add an element
	 * 
	 * @param elements
	 *            array of elements to add
	 */
	public void add(Object... elements) {
		log("add " + describe(elements));
		mElements.addAll(Arrays.asList(elements));
	}

	public void saveTo(Bundle outState) {
		log("recordSnapshot");
		JSONEncoder enc = new JSONEncoder();
		enc.enterList();
		enc.encode(mElements.size());
		for (Object element : mElements) {
			if (element instanceof Map) {
				Map m = (Map) element;
				enc.encode(m);
			} else if (element instanceof ListView) {
				ListView lv = (ListView) element;
				enc.encode(lv.getFirstVisiblePosition());
			} else if (element instanceof ScrollView) {
				ScrollView sv = (ScrollView) element;
				int x = sv.getScrollX();
				int y = sv.getScrollY();
				enc.encode(x);
				enc.encode(y);
			} else
				throw new IllegalArgumentException("cannot handle element "
						+ element);
		}
		enc.exit();
		String json = enc.toString();
		log("persistSnapshot (JSON " + json + ") to bundle " + nameOf(outState));
		outState.putString(KEY_BUNDLE, json);
	}

	/**
	 * Restore state from previously stored JSON string
	 */
	public void restoreFrom(Bundle savedInstanceState) {
		String json = null;
		if (savedInstanceState != null)
			json = savedInstanceState.getString(KEY_BUNDLE);
		log("restoreViewsFromSnapshot, JSON: " + json);
		if (json == null)
			return;

		String jsonString = json;
		JSONParser parser = new JSONParser(jsonString);
		parser.enterList();

		int persistCount = parser.nextInt();
		if (persistCount != mElements.size()) {
			log("saved elements differs from actual;\n JSON state: " + json
					+ "\n elements:" + d(mElements) + "\n " + this);
			return;
		}

		for (Object element : mElements) {
			if (element instanceof Map) {
				Map m = (Map) element;
				log("attempting to get next object from parser as map: "
						+ describe(parser.peekNext()));
				Map m2 = (Map) parser.next();
				m.clear();
				m.putAll(m2);
			} else if (element instanceof ListView) {
				ListView lv = (ListView) element;
				int cursor = parser.nextInt();
				lv.setSelection(cursor);
			} else if (element instanceof ScrollView) {
				ScrollView sv = (ScrollView) element;
				sv.setScrollX(parser.nextInt());
				sv.setScrollY(parser.nextInt());
			} else
				throw new IllegalArgumentException("cannot handle element "
						+ element);
		}
	}

	private List mElements = new ArrayList();
	private boolean mLogging;
}
