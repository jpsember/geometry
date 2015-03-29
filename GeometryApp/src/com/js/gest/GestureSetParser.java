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

import static com.js.gest.StrokeSet.KEY_ALIAS;
import static com.js.gest.StrokeSet.KEY_NAME;
import static com.js.gest.StrokeSet.KEY_STROKES;

class GestureSetParser {

  /**
   * <pre>
   * 
   * Format of JSON file
   * 
   * gesture_set := [ entry* ]
   * 
   * Each entry is a map.  
   * 
   * Each map contains exactly one of NAME or ALIAS,
   * and exactly one of STROKES or USES.
   * 
   * "name" :"x"    
   *   assigns gesture the unique name "x"
   * "alias" : "x"
   *   assigns gesture a unique (random) name, and makes it an alias of gesture named "x"
   * "strokes" : [ [...],[...]..]
   *   defines the stroke set
   * "uses" : "x"
   *   makes the gesture use the same stroke as that of gesture with name "x"
   *   (which must have a "strokes" mapping)
   * "transform" : ["option"...]
   *   specify which transformations, if any, are to be applied to the strokes;
   *   options include "fliphorz","flipvert","flipboth","reverse"
   * 
   * These boolean mappings are also supported:
   * 
   * "fliphorz":true
   * "flipvert":true
   * "flipboth":true
   * "reverse":true
   * 
   * If present, they each generate another gesture entry, an alias for this one (or for
   * the one that this one is an alias of), with the particular transform added to this one's
   * "transform" options
   * 
   * </pre>
   */
  private static final String KEY_USES = "uses";
  private static final String KEY_TRANSFORM = "transform";
  private static final boolean SHOW_PREPROCESSING = false;

  public List<StrokeSet> parse(String script) throws JSONException {

    mParsedArray = JSONTools.parseArray(script);

    // Read all of the entries into our map
    populateMapFromArray();

    // Process all entries which contain actual strokes, instead of
    // referencing others
    processStrokes();

    // Process all entries which contain references to strokes
    processStrokeReferences();

    processAliases();

    processFlags();

    return populateOutputSet();
  }

  private List<StrokeSet> populateOutputSet() {
    List<StrokeSet> strokes = new ArrayList();
    for (String name : mNamedSets.keySet()) {
      ParseEntry parseEntry = mNamedSets.get(name);
      StrokeSet entry = parseEntry.strokeSet();
      entry = normalizeStrokeSet(entry);
      entry.freeze();
      strokes.add(entry);
    }
    return strokes;
  }

  private String generateNameForAlias(JSONObject map) throws JSONException {
    String originalName = map.optString(KEY_ALIAS);
    if (originalName.isEmpty())
      throw new JSONException("Entry has no name and is not an alias:\n" + map);
    String name = "$" + mUniquePrefixIndex + "_" + originalName;
    mUniquePrefixIndex++;
    return name;
  }

  private void preprocessUnpackAliasList(JSONObject map) throws JSONException {
    // Unpack alias list?
    Object aliasObject = map.opt(KEY_ALIAS);
    if (aliasObject == null)
      return;
    if (!(aliasObject instanceof JSONArray))
      return;
    if (SHOW_PREPROCESSING)
      pr("Unpacking alias list: " + map);
    JSONArray array = (JSONArray) aliasObject;
    String originalName = array.getString(0);
    map.put(KEY_ALIAS, originalName);
    map.put(KEY_USES, originalName);
    JSONArray transform = (JSONArray) map.optJSONArray(KEY_TRANSFORM);
    if (transform == null) {
      transform = new JSONArray();
      map.put(KEY_TRANSFORM, transform);
    }
    for (int i = 1; i < array.length(); i++)
      transform.put(array.getString(i));
    if (SHOW_PREPROCESSING)
      pr(" result: " + map);
  }

  private void preprocessUnpackUsesList(JSONObject map) throws JSONException {
    // Unpack uses list?
    Object usesObject = map.opt(KEY_USES);
    if (usesObject == null)
      return;
    if (!(usesObject instanceof JSONArray))
      return;
    if (SHOW_PREPROCESSING)
      pr("Unpacking uses list: " + map);
    JSONArray array = (JSONArray) usesObject;
    String sourceName = array.getString(0);
    map.put(KEY_USES, sourceName);
    JSONArray transform = (JSONArray) map.optJSONArray(KEY_TRANSFORM);
    if (transform == null) {
      transform = new JSONArray();
      map.put(KEY_TRANSFORM, transform);
    }
    for (int i = 1; i < array.length(); i++)
      transform.put(array.getString(i));
    if (SHOW_PREPROCESSING)
      pr(" result: " + map);
  }

  private static String[] sTransformOptions = { "reverse", "fliphorz",
      "flipvert", "flipboth" };

  private void preprocessTransformedVersions(JSONObject originalMap)
      throws JSONException {
    for (String transformOption : sTransformOptions) {
      if (!originalMap.optBoolean(transformOption))
        continue;
      if (SHOW_PREPROCESSING)
        pr("Generating transformed version for " + transformOption + ": "
            + originalMap);

      // Remove this mapping from the original
      originalMap.remove(transformOption);

      /*
       * If present, they each generate another gesture entry, an alias for this
       * one (or for the one that this one is an alias of), with the particular
       * transform added to this one's "transform" options
       */
      String aliasName = originalMap.optString(KEY_ALIAS);
      if (aliasName.isEmpty()) {
        aliasName = originalMap.optString(KEY_NAME);
        if (aliasName.isEmpty())
          throw new JSONException("missing name");
      }

      JSONObject newMap = new JSONObject();

      newMap.put(KEY_ALIAS, aliasName);

      JSONArray array = originalMap.optJSONArray(KEY_STROKES);
      if (array != null) {
        // Note: we are storing multiple references to a single array here; we
        // will assume it's immutable
        newMap.put(KEY_STROKES, array);
      } else {
        String usesName = originalMap.optString(KEY_USES);
        if (usesName.isEmpty())
          if (usesName.isEmpty())
            throw new JSONException("missing 'uses'");
        newMap.put(KEY_USES, usesName);
      }

      // Make a copy of the existing transform values (or an empty list, if
      // there are none)
      JSONArray originalTransformOptions = (JSONArray) originalMap
          .optJSONArray(KEY_TRANSFORM);
      if (originalTransformOptions == null) {
        originalTransformOptions = new JSONArray();
      }
      JSONArray newTransformOptions = new JSONArray();
      for (int i = 0; i < originalTransformOptions.length(); i++)
        newTransformOptions.put(originalTransformOptions.get(i));

      // Add this transform to the list
      newTransformOptions.put(transformOption);

      newMap.put(KEY_TRANSFORM, newTransformOptions);
      if (SHOW_PREPROCESSING)
        pr(" result: " + newMap);

      mParsedArray.put(newMap);
    }
  }

  /**
   * Perform preprocessing on a gesture entry, if appropriate. These are
   * rewritings that allow the user more flexibility in the JSON representation
   * of a gesture.
   * 
   * <pre>
   * 
   * Unpack alias list 
   * --------------------------
   * "alias":["NAME", options...] =>
   *      "alias":"NAME", "uses":"NAME", "transform":[options...]
   * 
   * Unpack uses list
   * --------------------------
   * "uses":["NAME", options...] =>
   *      "uses":"NAME", "transform":[options...]
   *      
   * Add transformed aliases
   * --------------------------
   * { "name":"NAME", "reverse":true, "transform":[existing...] ... } =>
   *    { "name":"NAME", ... },
   *    { "alias":"NAME", "uses":"NAME", "transform":[existing... "reverse"] ... }
   * 
   * </pre>
   */
  private void preprocessEntry(JSONObject map) throws JSONException {
    preprocessUnpackAliasList(map);
    preprocessUnpackUsesList(map);
    preprocessTransformedVersions(map);
  }

  private void populateMapFromArray() throws JSONException {

    mNamedSets = new HashMap();

    // The array may grow as we perform rewritings, so avoid using an iterator
    for (int i = 0; i < mParsedArray.length(); i++) {
      JSONObject map = mParsedArray.getJSONObject(i);
      preprocessEntry(map);

      // If it has a NAME entry, use it; otherwise, it must have an alias; take
      // the name of the alias and prepend a unique prefix, and store that as
      // the name
      String name = map.optString(KEY_NAME);
      if (name.isEmpty()) {
        name = generateNameForAlias(map);
      } else {
        if (map.has(KEY_ALIAS))
          throw new JSONException("Entry has both a name and an alias: " + map);
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
      JSONArray strokes = entry.map().optJSONArray(KEY_STROKES);
      if (strokes == null)
        continue;
      StrokeSet strokeSet = parseStrokeSet(entry.strokeSet(), strokes);
      strokeSet = modifyExistingStrokeSet(strokeSet, strokeSet, entry.map()
          .optJSONArray(KEY_TRANSFORM));
      entry.setStrokeSet(strokeSet);
    }
  }

  private void processStrokeReferences() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry parseEntry = mNamedSets.get(name);
      JSONObject entryMap = parseEntry.map();
      String usesName = entryMap.optString(KEY_USES);
      if (usesName.isEmpty())
        continue;

      ParseEntry usesEntry = mNamedSets.get(usesName);
      if (usesEntry == null)
        throw new JSONException("No set found: " + usesName);
      StrokeSet usesSet = usesEntry.strokeSet();
      if (usesSet == null)
        throw new JSONException("No strokes found for: " + usesName);

      StrokeSet set = parseEntry.strokeSet();
      set = modifyExistingStrokeSet(set, usesSet,
          entryMap.optJSONArray(KEY_TRANSFORM));
      parseEntry.setStrokeSet(set);
    }
  }

  /**
   * Set alias fields for any entries that have been declared as such
   */
  private void processAliases() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry entry = mNamedSets.get(name);
      String aliasName = entry.map().optString(KEY_ALIAS);
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

  private void processFlags() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry entry = mNamedSets.get(name);
      JSONObject map = entry.map();
      if (map.optBoolean(StrokeSet.KEY_UNUSED))
        entry.strokeSet().setUnused(true);
      if (map.optBoolean(StrokeSet.KEY_DIRECTED))
        entry.strokeSet().setDirected(true);
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
    StrokeSet set2 = StrokeSet.buildFromStrokes(strokes);
    set2.inheritMetaDataFrom(set);
    return set2;
  }

  private static StrokeSet normalizeStrokeSet(StrokeSet set) {
    StrokeSet smoothedSet = frozen(set);
    smoothedSet = smoothedSet.fitToRect(null);
    StrokeSet normalizedSet = smoothedSet;
    normalizedSet = normalizedSet.normalize();
    return normalizedSet;
  }

  /**
   * Modify an existing stroke set according to some options
   * 
   * @param transformOptions
   *          if not null, list of transformation options
   */
  private StrokeSet modifyExistingStrokeSet(StrokeSet set, StrokeSet usesSet,
      JSONArray transformOptions) throws JSONException {
    Set<String> transformations = new HashSet();
    if (transformOptions != null) {
      for (int i = 0; i < transformOptions.length(); i++) {
        String opt = transformOptions.getString(i);
        transformations.add(opt);
      }
    }
    boolean reverse = transformations.contains("reverse");
    boolean flipHorz = transformations.contains("fliphorz")
        || transformations.contains("flipboth");
    boolean flipVert = transformations.contains("flipvert")
        || transformations.contains("flipboth");

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
    StrokeSet set2 = StrokeSet.buildFromStrokes(modifiedStrokes);
    set2.inheritMetaDataFrom(set);
    return set2;
  }

  private Map<String, ParseEntry> mNamedSets;
  private int mUniquePrefixIndex;
  private JSONArray mParsedArray;

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
