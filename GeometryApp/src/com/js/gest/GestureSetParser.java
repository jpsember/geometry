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
import com.js.gest.Stroke.DataPoint;

import static com.js.basic.Tools.*;

class GestureSetParser {

  /**
   * <pre>
   * 
   * Format of JSON file
   * 
   * gesture_set := [ entry* ]
   * 
   * Each entry is a map.  Each map contains exactly one of NAME or ALIAS,
   * and exactly one of STROKES or USES.
   * 
   * </pre>
   */
  private static final String KEY_USES = "uses";

  public void parse(String script, GestureSet collection) throws JSONException {

    JSONArray array = JSONTools.parseArray(script);

    // Pass 1: read all of the entries into our map
    populateMapFromArray(array);

    // Pass 2: process all entries which contain actual strokes, instead of
    // referencing others
    processStrokes();

    // Process all entries which contain references to strokes
    processStrokeReferences();

    processAliases();

    populateOutputSet(collection);
  }

  private void populateOutputSet(GestureSet collection) {
    for (String name : mNamedSets.keySet()) {
      ParseEntry parseEntry = mNamedSets.get(name);
      StrokeSet entry = parseEntry.strokeSet();
      entry.freeze();
      collection.add(entry);
    }
  }

  private String generateNameForAlias(JSONObject map) throws JSONException {
    String originalName = map.optString(StrokeSet.KEY_ALIAS);
    if (originalName.isEmpty())
      throw new JSONException("Entry has no name and is not an alias:\n" + map);
    String name = "$" + mUniquePrefixIndex;
    mUniquePrefixIndex++;
    return name;
  }

  /**
   * Perform preprocessing on a gesture entry, if appropriate
   * 
   * "alias":["name", options,....] =>
   * 
   * "alias":"name", "uses":["name", options, ...]
   * 
   */
  private void preprocessEntry(JSONObject map) throws JSONException {
    Object aliasObject = map.opt(StrokeSet.KEY_ALIAS);
    if (aliasObject != null) {
      if (aliasObject instanceof JSONArray) {
        JSONArray array = (JSONArray) aliasObject;
        String originalName = array.getString(0);
        map.put(StrokeSet.KEY_ALIAS, originalName);
        map.put(KEY_USES, array);
      }
    }
  }

  private void populateMapFromArray(JSONArray array) throws JSONException {

    mNamedSets = new HashMap();

    for (int i = 0; i < array.length(); i++) {
      JSONObject map = array.getJSONObject(i);

      preprocessEntry(map);

      // If it has a NAME entry, use it; otherwise, it must have an alias; take
      // the name of the alias and prepend a unique prefix, and store that as
      // the name
      String name = map.optString(StrokeSet.KEY_NAME);
      if (name.isEmpty()) {
        name = generateNameForAlias(map);
      } else {
        if (map.has(StrokeSet.KEY_ALIAS))
          throw new JSONException("Entry has both a name and an alias");
      }

      if (mNamedSets.containsKey(name))
        throw new JSONException("Duplicate name: " + name);
      ParseEntry parseEntry = new ParseEntry(name, map);
      mNamedSets.put(name, parseEntry);
    }
  }

  private void processStrokes() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry entry = mNamedSets.get(name);
      JSONArray strokes = entry.map().optJSONArray(StrokeSet.KEY_STROKES);
      if (strokes == null)
        continue;
      StrokeSet strokeSet = parseStrokeSet(entry.strokeSet(), strokes);
      entry.setStrokeSet(strokeSet);
    }
  }

  private void processStrokeReferences() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry parseEntry = mNamedSets.get(name);

      Set<String> options = new HashSet();
      String usesName = null;

      JSONArray usesList = parseEntry.map().optJSONArray(KEY_USES);
      if (usesList == null)
        continue;
      if (usesList.length() == 0)
        throw new JSONException("no uses name found: " + name);
      usesName = usesList.getString(0);
      for (int i = 1; i < usesList.length(); i++)
        options.add(usesList.getString(i));

      ParseEntry usesEntry = mNamedSets.get(usesName);
      if (usesEntry == null)
        throw new JSONException("No set found: " + usesName);
      StrokeSet usesSet = usesEntry.strokeSet();
      if (usesSet == null)
        throw new JSONException("No strokes found for: " + usesName);

      StrokeSet strokeSet2 = modifyExistingStrokeSet(parseEntry.strokeSet(),
          usesSet, options);
      parseEntry.setStrokeSet(strokeSet2);
    }
  }

  private void processAliases() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry entry = mNamedSets.get(name);
      String aliasName = entry.map().optString(StrokeSet.KEY_ALIAS);
      if (aliasName.isEmpty())
        continue;
      ParseEntry targetEntry = mNamedSets.get(aliasName);
      if (targetEntry == null)
        throw new JSONException("alias references unknown entry: " + name);
      StrokeSet set = mutableCopyOf(entry.strokeSet());
      set.setAliasName(aliasName);
      entry.setStrokeSet(set);
    }
  }

  private static StrokeSet parseStrokeSet(StrokeSet set, JSONArray array)
      throws JSONException {

    List<Stroke> strokes = new ArrayList();
    if (array.length() == 0)
      throw new JSONException("no strokes defined for " + set.name());
    for (int i = 0; i < array.length(); i++) {
      JSONArray strokeArray = array.getJSONArray(i);
      Stroke stroke = Stroke.parseJSONArray(strokeArray);
      strokes.add(stroke);
    }
    set = StrokeSet.buildFromStrokes(strokes, set);
    return normalizeStrokeSet(set);
  }

  private static StrokeSet normalizeStrokeSet(StrokeSet set) {
    final boolean withNormalizing = true;
    StrokeSet smoothedSet = frozen(set);
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
   */
  private StrokeSet modifyExistingStrokeSet(StrokeSet set, StrokeSet usesSet,
      Set<String> options) {
    if (sLegalOptions == null) {
      sLegalOptions = new HashSet();
      sLegalOptions.add("reverse");
      sLegalOptions.add("fliphorz");
      sLegalOptions.add("flipvert");
    }
    if (!sLegalOptions.containsAll(options))
      throw new IllegalArgumentException("illegal options for " + set.name()
          + ": " + d(options));

    boolean reverse = options.contains("reverse");
    boolean flipHorz = options.contains("fliphorz");
    boolean flipVert = options.contains("flipvert");

    List<Stroke> modifiedStrokes = new ArrayList();
    List<DataPoint> workList = new ArrayList();
    for (Stroke s : usesSet) {
      Stroke modifiedStroke = new Stroke();
      modifiedStrokes.add(modifiedStroke);
      workList.clear();
      float totalTime = s.totalTime();
      for (DataPoint spt : s) {
        float time = spt.getTime();
        Point pt = new Point(spt.getPoint());
        if (reverse)
          time = totalTime - time;
        if (flipHorz)
          pt.x = StrokeSet.sStandardRect.width - pt.x;
        if (flipVert)
          pt.y = StrokeSet.sStandardRect.height - pt.y;
        workList.add(new DataPoint(time, pt));
      }
      if (reverse)
        Collections.reverse(workList);
      for (DataPoint pt : workList)
        modifiedStroke.addPoint(pt);
    }
    set = StrokeSet.buildFromStrokes(modifiedStrokes, set);
    return set;
  }

  private Map<String, ParseEntry> mNamedSets;
  private int mUniquePrefixIndex;

  private static class ParseEntry {
    public ParseEntry(String name, JSONObject jsonMap) {
      mStrokeSet = new StrokeSet(name);
      mJSONMap = jsonMap;
    }

    public JSONObject map() {
      return mJSONMap;
    }

    public StrokeSet strokeSet() {
      return mStrokeSet;
    }

    public void setStrokeSet(StrokeSet set) {
      mStrokeSet = set;
    }

    private StrokeSet mStrokeSet;
    private JSONObject mJSONMap;
  }

}
