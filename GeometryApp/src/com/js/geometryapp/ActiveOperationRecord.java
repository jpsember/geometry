package com.js.geometryapp;

/**
 * Encapsulates state for an active operation, to allow user to page through
 * several of them within a single app. These active operations are either
 * algorithms, or the editor.
 */
class ActiveOperationRecord {

	/**
	 * Specify delegate, if operation is an algorithm
	 */
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
		if (isAlgorithm())
			return mDelegate.getAlgorithmName();
		else
			return "Editor";
	}

	@Override
	public String toString() {
		return "AlgorithmRecord[" + name() + "]";
	}

	/**
	 * Determine if this operation is an algorithm (vs, e.g., editor)
	 */
	public boolean isAlgorithm() {
		return mDelegate != null;
	}

	private Algorithm mDelegate;
	private WidgetGroup mWidgets;
}
