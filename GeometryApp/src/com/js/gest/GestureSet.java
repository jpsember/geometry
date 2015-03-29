package com.js.gest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;

import com.js.basic.Files;
import com.js.basic.MyMath;

import static com.js.basic.Tools.*;
import java.util.Comparator;

public class GestureSet {

  public static final String GESTURE_TAP = "*tap*";

  /**
   * Construct GestureSet from a list of StrokeSets
   */
  GestureSet(List<StrokeSet> gestures) {
    mMatcher = new StrokeSetMatcher(mStats);
    Random random = new Random(1965);
    for (StrokeSet set : gestures) {
      set = frozen(set);
      set.assertNamed();
      // If the library is currently empty, set its length to the length of this
      // set's strokes
      if (mEntriesMap.isEmpty()) {
        mStrokeLength = set.length();
      }

      // Construct a SortEntry for this gesture, and add it to the map
      SortEntry entry = new SortEntry(set, random.nextFloat());
      mEntriesMap.put(set.name(), entry);
    }
  }

  public static GestureSet parseJSON(String script) throws JSONException {
    GestureSetParser p = new GestureSetParser();
    List<StrokeSet> strokes = p.parse(script);
    GestureSet collection = new GestureSet(strokes);
    return collection;
  }

  public static GestureSet readFromClassResource(Class klass, String filename)
      throws IOException, JSONException {
    InputStream stream = klass.getResourceAsStream(filename);
    String json = Files.readString(stream);
    return GestureSet.parseJSON(json);
  }

  /**
   * Get the set of gesture names
   */
  public Set<String> getNames() {
    return Collections.unmodifiableSet(mEntriesMap.keySet());
  }

  /**
   * Get gesture
   * 
   * @param name
   *          name of gesture to look for
   * @return gesture, or null
   */
  public StrokeSet get(String name) {
    SortEntry entry = mEntriesMap.get(name);
    if (entry == null)
      return null;
    return entry.strokeSet();
  }

  /**
   * Get the length of the strokes used in this set's gestures
   */
  public int strokeLength() {
    return mStrokeLength;
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
    mMatcher.setMaximumCost(StrokeMatcher.INFINITE_COST);

    Set<SortEntry> sortedGestureSet = new TreeSet(SortEntry.COMPARATOR);
    sortedGestureSet.addAll(mEntriesMap.values());

    for (SortEntry gestureEntry : sortedGestureSet) {
      StrokeSet gesture = gestureEntry.strokeSet();
      if (gesture.size() != inputSet.size())
        continue;

      mMatcher.setArguments(gesture, inputSet, param);
      Match match = new Match(gesture, mMatcher.cost());
      results.add(match);

      // Update the cutoff value to be some small multiple of the smallest (raw)
      // cost yet seen.
      // Scale the raw cost by the number of strokes since the cost of the set
      // is the sum of the costs of the individual strokes.
      float newLimit = mMatcher.cost() / inputSet.size();
      newLimit *= param.maximumCostRatio();
      mMatcher.setMaximumCost(Math.min(newLimit, mMatcher.getMaximumCost()));
      if (mTrace && mMatcher.cost() < 20000) {
        pr(" gesture: " + d(gesture.name(), "15p") + " cost:"
            + dumpCost(mMatcher.cost()) + " max:"
            + dumpCost(mMatcher.getMaximumCost()) + "\n" + mStats);
      }

      trimResultsSet(results, param.maxResults());
    }

    if (results.isEmpty())
      return null;

    if (results.first().strokeSet().isUnused())
      return null;

    if (resultsList != null) {
      resultsList.addAll(results);
    }
    Match result = results.first();
    moveGestureToHeadOfList(result.strokeSet().name());
    return result;
  }

  private void moveGestureToHeadOfList(String name) {
    SortEntry entry = mEntriesMap.get(name);
    mUniqueIndex++;
    entry.setValue(MyMath.myMod(entry.value(), 1) + mUniqueIndex);
  }

  public AlgorithmStats getStats() {
    return mStats;
  }

  /**
   * Trim a sorted list of matches:
   * 
   * 1) while second best result is an alias of the first, remove it
   * 
   * 2) trim list size to maximum length
   */
  private void trimResultsSet(TreeSet<Match> results, int maxResults) {
    removeExtraneousAliasFromResults(results);
    // Throw out all but top k results
    while (results.size() > maxResults)
      results.pollLast();
  }

  /**
   * If the first two (sorted) entries are alias of the same gesture, throw out
   * the second one, since it cannot influence a match decision
   * 
   * @param sortedMatchSet
   */
  private void removeExtraneousAliasFromResults(TreeSet<Match> sortedMatchSet) {
    while (sortedMatchSet.size() >= 2) {
      Iterator<Match> iter = sortedMatchSet.iterator();
      Match m1 = iter.next();
      Match m2 = iter.next();
      if (!m1.strokeSet().aliasName().equals(m2.strokeSet().aliasName()))
        break;
      sortedMatchSet.remove(m2);
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
        diff = strokeSet().name().compareTo(m.strokeSet().name());
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

  /**
   * Entries for sorting StrokeSets by most-recently-used order
   */
  private static class SortEntry {

    public static final Comparator COMPARATOR = new Comparator() {
      @Override
      public int compare(Object a, Object b) {
        SortEntry aEnt = (SortEntry) a;
        SortEntry bEnt = (SortEntry) b;
        int diff = (int) Math.signum(bEnt.mValue - aEnt.mValue);
        if (diff == 0)
          diff = aEnt.strokeSet().name().compareTo(bEnt.strokeSet().name());
        return diff;
      }
    };

    public SortEntry(StrokeSet strokeSet, float value) {
      mStrokeSet = strokeSet;
      mValue = value;
    }

    public StrokeSet strokeSet() {
      return mStrokeSet;
    }

    /**
     * The values are positive floats, whose fractional parts are random, and
     * whose integer parts are unique and are higher for the more
     * recently-recognized gestures
     */
    public void setValue(float value) {
      mValue = value;
    }

    public float value() {
      return mValue;
    }

    private float mValue;
    private StrokeSet mStrokeSet;
  }

  // Map of gesture name -> SortEntry
  private Map<String, SortEntry> mEntriesMap = new HashMap();
  private int mStrokeLength;
  private boolean mTrace;
  private AlgorithmStats mStats = new AlgorithmStats();
  private StrokeSetMatcher mMatcher;
  private int mUniqueIndex;
}
