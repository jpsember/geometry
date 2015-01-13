package com.js.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONTools {

  /**
   * Parse a JSON map from a string
   */
  public static JSONObject parseMap(String source) throws JSONException {
    return (JSONObject) new JSONTokener(source).nextValue();
  }

  /**
   * Get keys from JSONObject as an Iterable<String>
   * 
   * @param object
   * @return
   */
  public static Iterable<String> keys(JSONObject object) {
    return toList((Iterator<String>) object.keys());
  }

  /**
   * Get Iterable from Set<String>
   */
  public static Iterable<String> iterable(Set<String> set) {
    return toList(set.iterator());
  }

  /**
   * Construct a list from an iterator
   */
  private static <T> List<T> toList(Iterator<T> iter) {
    List list = new ArrayList();
    while (iter.hasNext())
      list.add(iter.next());
    return list;
  }

  /**
   * Store a float within a JSON array as an integer, preserving 3 decimal
   * places
   * 
   * @param array
   * @param floatValue
   * @throws JSONException
   */
  public static void put(JSONArray array, float floatValue)
      throws JSONException {
    long intValue = (long) (floatValue * 1000);
    array.put(intValue);
  }

  public static float getFloat(JSONArray array, int cursor)
      throws JSONException {
    long intValue = array.getLong(cursor);
    return ((float) intValue) / 1000;
  }

}
