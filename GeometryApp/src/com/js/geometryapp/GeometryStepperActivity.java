package com.js.geometryapp;

import com.js.android.MyActivity;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

public abstract class GeometryStepperActivity extends GeometryActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		doNothing();
		mAlgorithmStepper = AlgorithmStepper.sharedInstance();
		hideTitle();

		super.onCreate(savedInstanceState);
		mAlgorithmStepper.restoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		mAlgorithmStepper.saveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		mAlgorithmStepper.destroy();
		super.onDestroy();
	}

	/**
	 * Hide the title bar, to conserve screen real estate
	 */
	private void hideTitle() {
		boolean hideTitle = true;
		// Checking this causes a null pointer exception if device is
		// rotated
		if (false) {
			DisplayMetrics m = MyActivity.displayMetrics();
			boolean landscapeMode = m.widthPixels > m.heightPixels;
			if (!landscapeMode)
				hideTitle = false;
		}
		if (hideTitle)
			getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	}

	public void setAlgorithmDelegate(AlgorithmStepper.Delegate delegate) {
		mAlgorithmStepper.setDelegate(delegate);
	}

	/**
	 * Construct a DrawerLayout that contains the main view and an auxilliary
	 * view
	 * 
	 * @param mainView
	 * @param auxilliaryView
	 * @deprecated
	 */
	private ViewGroup constructDrawerLayout(View mainView, View auxilliaryView) {
		DrawerLayout drawer = new DrawerLayout(this);

		DrawerLayout.LayoutParams lp = new DrawerLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);

		drawer.addView(mainView, lp);

		lp = new DrawerLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.START;
		drawer.addView(auxilliaryView, lp);

		return drawer;
	}

	/**
	 * Construct a SlidingPaneLayout that contains the main view and an
	 * auxilliary view
	 * 
	 * @param mainView
	 * @param auxilliaryView
	 */
	private ViewGroup constructSlidingPaneLayout(View mainView,
			View auxilliaryView) {

		SlidingPaneLayout container = new SlidingPaneLayout(this);

		// Determine if device in current orientation is large enough to display
		// both panes at once

		DisplayMetrics displayMetrics = MyActivity.displayMetrics();
		float mainWidth;
		float optionsWidth;

		final float PREFERRED_MAIN_WIDTH_INCHES = 3;
		final float PREFERRED_OPTIONS_WIDTH_INCHES = 2;
		final float TOTAL_WIDTH_INCHES = (PREFERRED_MAIN_WIDTH_INCHES + PREFERRED_OPTIONS_WIDTH_INCHES);

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
		SlidingPaneLayout.LayoutParams lp;
		lp = new SlidingPaneLayout.LayoutParams((int) mainWidth,
				LayoutParams.MATCH_PARENT);
		lp.weight = 1;

		container.addView(mainView, lp);

		lp = new SlidingPaneLayout.LayoutParams((int) optionsWidth,
				LayoutParams.MATCH_PARENT);
		lp.weight = 0;

		container.addView(auxilliaryView, lp);

		if (!bothFit)
			container.openPane();
		return container;
	}

	private ViewGroup constructOptions() {
		LinearLayout options = new LinearLayout(this);
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
				Button b = new Button(this);
				b.setText(labels[i]);
				b.setOnClickListener(listener);
				options.addView(b);
			}
		}
		return options;
	}

	protected ViewGroup buildContentView() {
		ViewGroup mainView = super.buildContentView();
		mainView.addView(mAlgorithmStepper.controllerView(this));

		if (false) {
			return mainView;
		} else {
			ViewGroup options = constructOptions();
			ViewGroup container;
			if (false) {
				container = constructDrawerLayout(mainView, options);
			} else {
				container = constructSlidingPaneLayout(mainView, options);
			}
			return container;
		}
	}

	protected final GLSurfaceView buildOpenGLView() {
		GLSurfaceView v = new OurGLSurfaceView(this, buildRenderer(this));
		v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		return v;
	}

	protected abstract AlgorithmRenderer buildRenderer(Context context);

	private AlgorithmStepper mAlgorithmStepper;
}
