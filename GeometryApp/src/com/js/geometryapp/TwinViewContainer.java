package com.js.geometryapp;

import com.js.android.MyActivity;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

/**
 * Wrapper for a SlidingPaneLayout, to preserve screen real estate on small
 * devices. It contains both a 'main' view, and an 'auxilliary' view that can
 * slide to temporarily obscure the main view.
 */
class TwinViewContainer {

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param mainView
	 *            the main view (i.e. the original 'content' view), it will
	 *            actually be placed within a SlidingPaneLayout, which becomes
	 *            the new content view
	 */
	TwinViewContainer(Context context, View mainView) {
		buildSlidingPane(context);
		buildAuxilliaryView();
		addChildViews(mainView);
	}

	/**
	 * Get the view that contains both the main and auxilliary views
	 */
	ViewGroup getContainer() {
		return mSlidingPane;
	}

	ViewGroup getAuxilliaryView() {
		return mAuxilliaryView;
	}

	/**
	 * Build a view that will appear in the sliding pane
	 */
	private void buildAuxilliaryView() {
		LinearLayout view = new LinearLayout(getContext());
		view.setOrientation(LinearLayout.VERTICAL);
		int padding = MyActivity.inchesToPixels(.06f);
		view.setPadding(padding, padding, padding, padding);
		mAuxilliaryView = view;
	}

	private void buildSlidingPane(Context context) {
		mSlidingPane = new SlidingPaneLayout(context);
	}

	/**
	 * Add main and aux views to the sliding pane
	 */
	private void addChildViews(View mainView) {

		// Determine if device in current orientation is large enough to display
		// both panes at once

		final float PREFERRED_MAIN_WIDTH_INCHES = 3;
		final float PREFERRED_AUX_WIDTH_INCHES = 2;
		final float TOTAL_WIDTH_INCHES = (PREFERRED_MAIN_WIDTH_INCHES + PREFERRED_AUX_WIDTH_INCHES);

		DisplayMetrics displayMetrics = MyActivity.displayMetrics();
		float mainWidth;
		float auxWidth;

		boolean landscapeMode = displayMetrics.widthPixels > displayMetrics.heightPixels;
		float deviceWidthInInches = displayMetrics.widthPixels
				/ displayMetrics.xdpi;
		boolean bothFit = landscapeMode
				&& deviceWidthInInches >= PREFERRED_MAIN_WIDTH_INCHES
						+ PREFERRED_AUX_WIDTH_INCHES;

		if (bothFit) {
			mainWidth = displayMetrics.widthPixels
					* (PREFERRED_MAIN_WIDTH_INCHES / TOTAL_WIDTH_INCHES);
			auxWidth = displayMetrics.widthPixels
					* (PREFERRED_AUX_WIDTH_INCHES / TOTAL_WIDTH_INCHES);
		} else {
			mainWidth = LayoutParams.MATCH_PARENT;
			auxWidth = LayoutParams.MATCH_PARENT;
		}

		SlidingPaneLayout.LayoutParams lp = new SlidingPaneLayout.LayoutParams(
				(int) mainWidth, LayoutParams.MATCH_PARENT);
		lp.weight = 1;
		mSlidingPane.addView(mainView, lp);

		lp = new SlidingPaneLayout.LayoutParams((int) auxWidth,
				LayoutParams.MATCH_PARENT);

		mSlidingPane.addView(mAuxilliaryView, lp);

		if (!bothFit)
			mSlidingPane.openPane();
	}

	private Context getContext() {
		return mSlidingPane.getContext();
	}

	private SlidingPaneLayout mSlidingPane;
	private ViewGroup mAuxilliaryView;

}
