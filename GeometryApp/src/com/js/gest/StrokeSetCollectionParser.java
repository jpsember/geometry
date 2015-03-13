package com.js.gest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.basic.JSONTools;
import com.js.basic.Point;
import static com.js.basic.Tools.*;

class StrokeSetCollectionParser {

	private static final String KEY_NAME = "name";
	private static final String KEY_ALIAS = "alias";
	private static final String KEY_REVERSE_ALIAS = "reverse_alias";
	private static final String KEY_SOURCE = "source";
	private static final String KEY_STROKES = "strokes";

	public void parse(String script, StrokeSetCollection collection)
			throws JSONException {

		mStrokeSetCollection = collection;
		JSONArray array = JSONTools.parseArray(script);
		// Pass 1: read all of the entries into our map
		populateMapFromArray(array);

		// Pass 2: process all entries which contain strokes (instead of
		// references to a source)
		processStrokes();

		// Process all entries which contain references to strokes
		processStrokeReferences();

		processAliases();
	}

	private static void quote(StringBuilder sb, String text) {
		sb.append('"');
		sb.append(text);
		sb.append('"');
	}

	public static String strokeSetToJSON(StrokeSet set, String name)
			throws JSONException {
		StringBuilder sb = new StringBuilder(",{");
		quote(sb, KEY_NAME);
		sb.append(':');
		quote(sb, name);
		sb.append(',');
		quote(sb, KEY_STROKES);
		sb.append(":[");
		for (int i = 0; i < set.size(); i++) {
			if (i != 0)
				sb.append(',');
			sb.append(set.get(i).toJSONArray());
		}
		sb.append("]}\n");
		return sb.toString();
	}

	private void populateMapFromArray(JSONArray array) throws JSONException {
		mNamedSets = new HashMap();
		for (int i = 0; i < array.length(); i++) {
			JSONObject map = array.getJSONObject(i);
			String name = getName(map);
			if (mNamedSets.containsKey(name))
				throw new JSONException("Duplicate name: " + name);
			verifyLegality(map);
			ParseEntry parseEntry = new ParseEntry(name, map);
			mNamedSets.put(name, parseEntry);
			mStrokeSetCollection.map().put(name, parseEntry.strokeSetEntry());
		}
	}

	private void verifyLegality(JSONObject map) throws JSONException {
		int count = 0;
		if (map.has(KEY_STROKES))
			count++;
		if (map.has(KEY_SOURCE))
			count++;
		if (map.has(KEY_REVERSE_ALIAS))
			count++;
		if (count != 1)
			throw new JSONException(getName(map) + " must have exactly one of "
					+ KEY_STROKES + ", " + KEY_SOURCE + ", " + KEY_REVERSE_ALIAS);
	}

	private void processStrokes() throws JSONException {
		for (String name : mNamedSets.keySet()) {
			ParseEntry entry = mNamedSets.get(name);
			JSONArray strokes = entry.map().optJSONArray(KEY_STROKES);
			if (strokes == null)
				continue;
			StrokeSet strokeSet = parseStrokeSet(name, strokes);
			entry.strokeSetEntry().addStrokeSet(strokeSet);
		}
	}

	private void processStrokeReferences() throws JSONException {
		for (String name : mNamedSets.keySet()) {
			ParseEntry parseEntry = mNamedSets.get(name);
			StrokeSetEntry strokeSetEntry = parseEntry.strokeSetEntry();

			Set<String> options = new HashSet();
			String sourceName = null;
			String revAliasName = parseEntry.map().optString(KEY_REVERSE_ALIAS);
			if (!revAliasName.isEmpty()) {
				sourceName = revAliasName;
				options.add("reverse");
			} else {
				JSONArray sourceList = parseEntry.map().optJSONArray(KEY_SOURCE);
				if (sourceList == null)
					continue;
				if (sourceList.length() == 0)
					throw new JSONException("no source name found: " + name);
				sourceName = sourceList.getString(0);
				for (int i = 1; i < sourceList.length(); i++)
					options.add(sourceList.getString(i));
			}

			ParseEntry sourceEntry = mNamedSets.get(sourceName);
			if (sourceEntry == null)
				throw new JSONException("No set found: " + sourceName);
			StrokeSet sourceSet = sourceEntry.strokeSetEntry().strokeSet();
			if (sourceSet == null)
				throw new JSONException("No strokes found for: " + sourceName);

			StrokeSet strokeSet = modifyExistingStrokeSet(name, sourceSet, options);
			strokeSetEntry.addStrokeSet(strokeSet);
		}
	}

	private void processAliases() throws JSONException {
		for (String name : mNamedSets.keySet()) {
			ParseEntry entry = mNamedSets.get(name);
			String aliasName = entry.map().optString(KEY_ALIAS);
			if (aliasName.isEmpty())
				aliasName = entry.map().optString(KEY_REVERSE_ALIAS);
			if (aliasName.isEmpty())
				continue;
			ParseEntry targetEntry = mNamedSets.get(aliasName);
			if (targetEntry == null || targetEntry.map().has(KEY_ALIAS))
				throw new JSONException("problem with alias: " + name);
			entry.strokeSetEntry().setAlias(targetEntry.strokeSetEntry());
		}
	}

	private static StrokeSet parseStrokeSet(String name, JSONArray array)
			throws JSONException {

		List<Stroke> strokes = new ArrayList();
		if (array.length() == 0)
			throw new JSONException("no strokes defined for " + name);
		for (int i = 0; i < array.length(); i++) {
			JSONArray setEntry = array.getJSONArray(i);
			Stroke stroke = Stroke.parseJSONArray(setEntry);
			strokes.add(stroke);
		}
		StrokeSet set = StrokeSet.buildFromStrokes(strokes);
		set = normalizeStrokeSet(set);
		return set;
	}

	private static StrokeSet normalizeStrokeSet(StrokeSet set) {
		final boolean withSmoothing = true;
		final boolean withNormalizing = true;
		StrokeSet smoothedSet = set;
		if (withSmoothing) {
			StrokeSmoother s = new StrokeSmoother(set);
			set = s.getSmoothedSet();
			smoothedSet = set;
		}
		smoothedSet = smoothedSet.fitToRect(null);

		StrokeSet normalizedSet = smoothedSet;
		if (withNormalizing) {
			normalizedSet = StrokeNormalizer.normalize(normalizedSet);
		}
		return normalizedSet;
	}

	private static Set<String> sLegalOptions;

	/**
	 * Modify an existing stroke set according to some options
	 * 
	 * Options can include:
	 * 
	 * 'reverse' : reverse the time sequence of the stroke points
	 * 
	 * 'fliphorz' : flip around y axis
	 * 
	 * 'flipvert' : flip around x axis
	 * 
	 * @param sourceSet
	 * @param options
	 * 
	 */
	private StrokeSet modifyExistingStrokeSet(String setName,
			StrokeSet sourceSet, Set<String> options) {
		if (sLegalOptions == null) {
			sLegalOptions = new HashSet();
			sLegalOptions.add("reverse");
			sLegalOptions.add("fliphorz");
			sLegalOptions.add("flipvert");
		}
		if (!sLegalOptions.containsAll(options))
			throw new IllegalArgumentException("illegal options for " + setName
					+ ": " + d(options));

		boolean reverse = options.contains("reverse");
		boolean flipHorz = options.contains("fliphorz");
		boolean flipVert = options.contains("flipvert");

		List<Stroke> modifiedStrokes = new ArrayList();
		List<StrokePoint> workList = new ArrayList();
		for (Stroke s : sourceSet) {
			Stroke modifiedStroke = new Stroke();
			modifiedStrokes.add(modifiedStroke);
			workList.clear();
			float totalTime = s.totalTime();
			for (StrokePoint spt : s) {
				float time = spt.getTime();
				Point pt = new Point(spt.getPoint());
				if (reverse)
					time = totalTime - time;
				if (flipHorz)
					pt.x = StrokeSet.sStandardRect.width - pt.x;
				if (flipVert)
					pt.y = StrokeSet.sStandardRect.height - pt.y;
				workList.add(new StrokePoint(time, pt));
			}
			if (reverse)
				Collections.reverse(workList);
			for (StrokePoint pt : workList)
				modifiedStroke.addPoint(pt);
		}
		return StrokeSet.buildFromStrokes(modifiedStrokes);
	}

	private static String getName(JSONObject strokeSetMap) throws JSONException {
		return strokeSetMap.getString(KEY_NAME);
	}

	private Map<String, ParseEntry> mNamedSets;
	private StrokeSetCollection mStrokeSetCollection;

	private static class ParseEntry {
		public ParseEntry(String name, JSONObject jsonMap) {
			mEntry = new StrokeSetEntry(name);
			mJSONMap = jsonMap;
		}

		public JSONObject map() {
			return mJSONMap;
		}

		public StrokeSetEntry strokeSetEntry() {
			return mEntry;
		}

		private StrokeSetEntry mEntry;
		private JSONObject mJSONMap;
	}

}
