package com.js.gest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONException;

import com.js.basic.Files;

import static com.js.basic.Tools.*;

public class GestureSet {

  public static final String GESTURE_TAP = "*tap*";

  public static GestureSet parseJSON(String script) throws JSONException {
    GestureSetParser p = new GestureSetParser();
    GestureSet collection = new GestureSet();
    p.parse(script, collection);
    return collection;
  }

  public static GestureSet readFromClassResource(Class klass, String filename)
      throws IOException, JSONException {
    InputStream stream = klass.getResourceAsStream(filename);
    String json = Files.readString(stream);
    return GestureSet.parseJSON(json);
  }

  /**
   * Get the map containing the gestures
   */
  private Map<String, StrokeSet> map() {
    return mEntriesMap;
  }

  /**
   * Get gesture
   * 
   * @param name
   *          name of gesture to look for
   * @return gesture, or null
   */
  public StrokeSet get(String name) {
    return map().get(name);
  }

  /**
   * Get the length of the strokes used in this set's gestures
   */
  public int strokeLength() {
    return mStrokeLength;
  }

  public void add(StrokeSet set) {
    set = frozen(set);
    set.assertNamed();
    // If the library is currently empty, set its length to the length of this
    // set's strokes
    if (mEntriesMap.isEmpty()) {
      mStrokeLength = set.length();
    }
    mEntriesMap.put(set.name(), set);
  }

  public void setTraceStatus(boolean trace) {
    mTrace = trace;
  }

  /**
   * Find a match for a gesture, if possible
   * 
   * @param inputSet
   *          gesture to examine
   * @param param
   *          parameters for match, or null to use defaults
   * @param resultsList
   *          optional list of highest candidate gestures; if not null, they
   *          will be returned in this list
   * @return match found, or null
   */
  public Match findMatch(StrokeSet inputSet, MatcherParameters param,
      List<Match> resultsList) {
    if (mTrace)
      pr("GestureSet findMatch");
    if (param == null)
      param = MatcherParameters.DEFAULT;
    if (resultsList != null)
      resultsList.clear();
    TreeSet<Match> results = new TreeSet();
    mMaximumCost = StrokeMatcher.INFINITE_COST;

    prepareAliasLowCostMap();

    for (String gestureName : mEntriesMap.keySet()) {
      StrokeSet gesture = mEntriesMap.get(gestureName);
      if (gesture.size() != inputSet.size())
        continue;

      mMatcher.setArguments(gesture, inputSet, param);
      setMaximumCost(gesture);
      Match match = new Match(gesture, mMatcher.cost());
      results.add(match);

      updateAliasLowCostMap(gesture);

      // Update the cutoff value to be some small multiple of the smallest (raw)
      // cost yet seen.
      // Scale the raw cost by the number of strokes since the cost of the set
      // is the sum of the costs of the individual strokes.
      float newLimit = mMatcher.cost() / inputSet.size();
      newLimit *= param.maximumCostRatio();
      mMaximumCost = Math.min(newLimit, mMaximumCost);
      if (mTrace) {
        pr(" gesture: " + d(gestureName, "15p") + " cost:"
            + dumpCost(mMatcher.cost()) + " max:" + dumpCost(mMaximumCost)
            + " cells %:"
            + d((int) (100 * mMatcher.strokeMatcher().cellsExaminedRatio())));
      }

      removeExtraneousAliasFromResults(results);

      // Throw out all but top three
      while (results.size() > 3)
        results.pollLast();
    }

    if (results.isEmpty())
      return null;

    if (results.first().strokeSet().isUnused())
      return null;

    if (resultsList != null) {
      resultsList.addAll(results);
    }

    return results.first();
  }

  /**
   * If the first two (sorted) entries are alias of the same gesture, throw out
   * the second one, since it cannot influence a match decision
   * 
   * @param sortedMatchSet
   */
  private void removeExtraneousAliasFromResults(TreeSet<Match> sortedMatchSet) {
    if (sortedMatchSet.size() < 2)
      return;
    Iterator<Match> iter = sortedMatchSet.iterator();
    Match m1 = iter.next();
    Match m2 = iter.next();
    if (m1.strokeSet().aliasName().equals(m2.strokeSet().aliasName())) {
      sortedMatchSet.remove(m2);
    }
  }

  /**
   * Set the maximum cost bound prior to matching
   * 
   * @param gesture
   *          candidate gesture
   */
  private void setMaximumCost(StrokeSet gesture) {
    float maximumCost = mMaximumCost;

    // If an alias of this gesture was previously examined, use that cost as a
    // hard upper bound (i.e. without a maximumCostRatio multiplier)
    Float aliasMinCost = mAliasLowCostMap.get(gesture.aliasName());
    if (aliasMinCost != null)
      maximumCost = Math.min(mMaximumCost, aliasMinCost);
    mMatcher.setMaximumCost(maximumCost);
  }

  private void prepareAliasLowCostMap() {
    mAliasLowCostMap.clear();
  }

  /**
   * Update the alias low cost map according to match that has just occurred
   */
  private void updateAliasLowCostMap(StrokeSet gesture) {
    float currentCost = mMatcher.cost();
    if (currentCost >= StrokeMatcher.INFINITE_COST)
      return;
    Float currentLowCost = mAliasLowCostMap.get(gesture.aliasName());
    if (currentLowCost == null || currentLowCost > currentCost) {
      if (mTrace) {
        if (currentLowCost != null)
          pr(" updating low cost for alias " + gesture.aliasName() + " to "
              + currentCost);
      }
      mAliasLowCostMap.put(gesture.aliasName(), currentCost);
    }
  }

  /**
   * Utility method for converting a (raw) cost value to a string
   */
  private static String dumpCost(float cost) {
    if (cost >= StrokeMatcher.INFINITE_COST)
      return " ********";
    return d(((int) cost), 8);
  }

  public static class Match implements Comparable {
    Match(StrokeSet set, float cost) {
      set.assertNamed();
      mStrokeSet = set;
      mCost = cost;
    }

    public float cost() {
      return mCost;
    }

    /**
     * Get StrokeSet
     */
    public StrokeSet strokeSet() {
      return mStrokeSet;
    }

    @Override
    public int compareTo(Object another) {
      Match m = (Match) another;
      int diff = (int) Math.signum(this.cost() - m.cost());
      if (diff == 0) {
        diff = String.CASE_INSENSITIVE_ORDER.compare(this.strokeSet().name(), m
            .strokeSet().name());
      }
      return diff;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(d((int) cost()));
      sb.append(' ');
      sb.append(strokeSet().name());
      return sb.toString();
    }

    private StrokeSet mStrokeSet;
    private float mCost;

  }

  public GestureSet buildWithStrokeLength(int strokeLength) {
    GestureSet library = new GestureSet();
    for (String name : map().keySet()) {
      StrokeSet entry = map().get(name);
      StrokeSet set2 = entry.normalize(strokeLength);
      library.add(set2);
    }
    return library;
  }

  private Map<String, StrokeSet> mEntriesMap = new HashMap();
  private int mStrokeLength;
  private boolean mTrace;

  // Current upper bound for match
  private float mMaximumCost;

  // Map to record the lowest costs found for any alias of a
  // particular gesture (so we use that cost as an upper bound for subsequent
  // comparisons)
  private Map<String, Float> mAliasLowCostMap = new HashMap();
  private StrokeSetMatcher mMatcher = new StrokeSetMatcher();
}
