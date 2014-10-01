package com.js.geometryapp;

/**
 * Encapsulates state for an algorithm, to allow user to page through several
 * algorithms in a single app
 */
public class AlgorithmRecord {

	public void setDelegate(Algorithm delegate) {
		mDelegate = delegate;
	}

	public void setWidgetGroup(WidgetGroup group) {
		mWidgets = group;
	}

	public Algorithm delegate() {
		return mDelegate;
	}

	public WidgetGroup widgets() {
		return mWidgets;
	}

	@Override
	public String toString() {
		return "AlgorithmRecord[" + delegate().getAlgorithmName() + "]";
	}

	private Algorithm mDelegate;
	private WidgetGroup mWidgets;
}
