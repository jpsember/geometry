package com.js.gest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONException;

import static com.js.basic.Tools.*;

public class StrokeSetCollection {

	public static StrokeSetCollection parseJSON(String script)
			throws JSONException {

		StrokeSetCollectionParser p = new StrokeSetCollectionParser();
		StrokeSetCollection collection = new StrokeSetCollection();
		p.parse(script, collection);
		return collection;
	}

	/**
	 * Get the map containing the entries
	 */
	public Map<String, StrokeSetEntry> map() {
		return mEntriesMap;
	}

	/**
	 * Convenience method for map().get()
	 */
	public StrokeSetEntry get(String name) {
		return map().get(name);
	}

	public void add(String name, StrokeSet set) {
		if (set == null)
			throw new IllegalArgumentException();
		StrokeSetEntry entry = mEntriesMap.get(name);
		if (entry == null) {
			entry = new StrokeSetEntry(name);
			mEntriesMap.put(name, entry);
		}
		entry.addStrokeSet(set);
	}

	public Match findMatch(StrokeSet inputSet, MatcherParameters param) {
		return findMatch(inputSet, null, param);
	}

	public Match findMatch(StrokeSet inputSet, List<Match> resultsList,
			MatcherParameters param) {
		if (resultsList != null)
			resultsList.clear();
		TreeSet<Match> results = new TreeSet();
		for (String setName : mEntriesMap.keySet()) {
			StrokeSetEntry entry = mEntriesMap.get(setName);
			StrokeSet set2 = entry.strokeSet(inputSet.length());
			if (set2.size() != inputSet.size())
				continue;

			StrokeSetMatcher m = new StrokeSetMatcher(set2, inputSet, param);
			Match match = new Match(entry, m.similarity());
			results.add(match);
			// Throw out all but top three
			while (results.size() > 3)
				results.pollLast();
		}
		if (results.isEmpty())
			return null;

		if (resultsList != null) {
			resultsList.addAll(results);
		}

		return results.first();
	}

	public static class Match implements Comparable {
		public Match(StrokeSetEntry set, float cost) {
			mStrokeSetEntry = set;
			mCost = cost;
		}

		public float cost() {
			return mCost;
		}

		public StrokeSetEntry setEntry() {
			return mStrokeSetEntry;
		}

		@Override
		public int compareTo(Object another) {
			Match m = (Match) another;
			int diff = (int) Math.signum(this.cost() - m.cost());
			if (diff == 0) {
				diff = String.CASE_INSENSITIVE_ORDER.compare(this.setEntry().name(), m
						.setEntry().name());
				if (diff != 0) {
					warning("comparisons matched exactly, this is unlikely: "
							+ this.setEntry().name()
							+ " / "
							+ m.setEntry().name()
							+ "\n and may be indicative of a spelling mistake in the 'source' options");
				}
			}
			return diff;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(d(cost()));
			sb.append(' ');
			sb.append(setEntry().name());
			if (setEntry().hasAlias())
				sb.append(" --> " + setEntry().aliasName());
			return sb.toString();
		}

		private StrokeSetEntry mStrokeSetEntry;
		private float mCost;

	}

	private Map<String, StrokeSetEntry> mEntriesMap = new HashMap();
}
