package com.js.geometryapp;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.js.android.MyActivity;
import static com.js.basic.Tools.*;

/**
 * Encapsulates the user-defined options. These appear in a SlidingPaneLayout,
 * to preserve screen real estate on small devices
 */
public class AlgorithmOptions {

	/**
	 * Construct an options object
	 * 
	 * @param context
	 * @param mainView
	 *            the main view (i.e. the original 'content' view), it will
	 *            actually be placed within the options SlidingPaneLayout, which
	 *            becomes the new content view
	 */
	public static AlgorithmOptions construct(Context context, View mainView) {
		AlgorithmOptions v = new AlgorithmOptions();
		v.buildSlidingPane(context);
		v.buildOptionsView();
		v.addChildViews(mainView);
		sAlgorithmOptions = v;
		return v;
	}

	/**
	 * Get the singleton instance of the options object
	 */
	public static AlgorithmOptions sharedInstance() {
		return sAlgorithmOptions;
	}

	/**
	 * Get the view that contains both the main view and the options
	 */
	public ViewGroup getView() {
		return mSlidingPane;
	}

	/**
	 * Private constructor
	 */
	private AlgorithmOptions() {
	}

	private void buildSlidingPane(Context context) {
		mSlidingPane = new SlidingPaneLayout(context);
	}

	/**
	 * Add the main content view and options views to the sliding pane
	 */
	private void addChildViews(View mainView) {

		// Determine if device in current orientation is large enough to display
		// both panes at once

		final float PREFERRED_MAIN_WIDTH_INCHES = 3;
		final float PREFERRED_OPTIONS_WIDTH_INCHES = 2;
		final float TOTAL_WIDTH_INCHES = (PREFERRED_MAIN_WIDTH_INCHES + PREFERRED_OPTIONS_WIDTH_INCHES);

		DisplayMetrics displayMetrics = MyActivity.displayMetrics();
		float mainWidth;
		float optionsWidth;

		boolean landscapeMode = displayMetrics.widthPixels > displayMetrics.heightPixels;
		float deviceWidthInInches = displayMetrics.widthPixels
				/ displayMetrics.xdpi;
		boolean bothFit = landscapeMode
				&& deviceWidthInInches >= PREFERRED_MAIN_WIDTH_INCHES
						+ PREFERRED_OPTIONS_WIDTH_INCHES;

		if (bothFit) {
			mainWidth = displayMetrics.widthPixels
					* (PREFERRED_MAIN_WIDTH_INCHES / TOTAL_WIDTH_INCHES);
			optionsWidth = displayMetrics.widthPixels
					* (PREFERRED_OPTIONS_WIDTH_INCHES / TOTAL_WIDTH_INCHES);
		} else {
			mainWidth = LayoutParams.MATCH_PARENT;
			optionsWidth = LayoutParams.MATCH_PARENT;
		}

		SlidingPaneLayout.LayoutParams lp = new SlidingPaneLayout.LayoutParams(
				(int) mainWidth, LayoutParams.MATCH_PARENT);
		lp.weight = 1;
		mSlidingPane.addView(mainView, lp);

		lp = new SlidingPaneLayout.LayoutParams((int) optionsWidth,
				LayoutParams.MATCH_PARENT);
		ASSERT(lp.weight == 0);
		mSlidingPane.addView(mOptionsView, lp);

		if (!bothFit)
			mSlidingPane.openPane();
	}

	private Context getContext() {
		return mSlidingPane.getContext();
	}

	/**
	 * Build a view to contain the various user controls (buttons, checkboxes,
	 * etc) that will appear in the sliding pane
	 */
	private void buildOptionsView() {
		LinearLayout options = new LinearLayout(getContext());
		options.setOrientation(LinearLayout.VERTICAL);
		mOptionsView = options;
	}

	public Spinner addDropdown(String[] labels,
			AdapterView.OnItemSelectedListener listener) {
		Spinner spinner = new Spinner(getContext());

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_spinner_item, labels);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		mOptionsView.addView(spinner);
		if (listener != null)
			spinner.setOnItemSelectedListener(listener);
		return spinner;
	}

	private static AlgorithmOptions sAlgorithmOptions;

	private SlidingPaneLayout mSlidingPane;
	private ViewGroup mOptionsView;
}
