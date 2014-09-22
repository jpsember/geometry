package com.js.geometryapp;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

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
		return v;
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

		{
			View.OnClickListener listener = new View.OnClickListener() {
				public void onClick(View v) {
					AlgorithmStepper.sharedInstance()
							.processTestButtonPressed();
				}
			};

			// Add some buttons
			String labels[] = { "Alpha", "Bravo", "Charlie" };
			for (int i = 0; i < labels.length; i++) {
				Button b = new Button(getContext());
				b.setText(labels[i]);
				b.setOnClickListener(listener);
				options.addView(b);
			}
		}
		mOptionsView = options;
	}

	private SlidingPaneLayout mSlidingPane;
	private View mOptionsView;
}
