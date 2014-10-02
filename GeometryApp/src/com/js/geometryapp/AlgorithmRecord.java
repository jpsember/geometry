package com.js.geometryapp;

/**
 * Encapsulates state for an algorithm, to allow user to page through several
 * algorithms in a single app
 */
class AlgorithmRecord {

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

	public String name() {
		return mDelegate.getAlgorithmName();
	}

	@Override
	public String toString() {
		return "AlgorithmRecord[" + name() + "]";
	}

	private Algorithm mDelegate;
	private WidgetGroup mWidgets;
}
