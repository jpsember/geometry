package com.js.gest;

import java.util.HashMap;
import java.util.Map;
import static com.js.basic.Tools.*;

public class StrokeSetEntry {

	/**
	 * Get stroke set of longest length
	 */
	public StrokeSet strokeSet() {
		StrokeSet best = null;
		for (StrokeSet set : mStrokeSetMap.values()) {
			if (best == null || best.length() < set.length())
				best = set;
		}
		if (best == null)
			throw new IllegalStateException("set is empty");
		return best;
	}

	public StrokeSet strokeSet(int desiredStrokeLength) {
		StrokeSet set = mStrokeSetMap.get(desiredStrokeLength);
		if (set == null)
			throw new IllegalArgumentException("no stroke set '" + name()
					+ "' of length " + desiredStrokeLength + " within " + nameOf(this));
		return set;
	}

	public String name() {
		return mName;
	}

	/**
	 * Get the name of the stroke set this one is an alias of; returns our name if
	 * we are not an alias
	 */
	public String aliasName() {
		if (mAliasName == null)
			return mName;
		return mAliasName;
	}

	public boolean hasAlias() {
		return mAliasName != null;
	}

	StrokeSetEntry(String name) {
		mName = name;
	}

	void addStrokeSet(StrokeSet strokeSet) {
		mStrokeSetMap.put(strokeSet.length(), strokeSet);
	}

	void setAlias(StrokeSetEntry strokeSetEntry) {
		mAliasName = strokeSetEntry.aliasName();
	}

	private String mAliasName;
	private String mName;
	// Map of stroke sets, keyed by normalization length
	private Map<Integer, StrokeSet> mStrokeSetMap = new HashMap();
}
