package com.js.geometryapp;

import com.js.android.UITools;
import com.js.geometry.R;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.ButtonWidget;
import com.js.geometryapp.widget.SliderWidget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * View that contains the step controls (slider and page / step buttons)
 */
class AlgorithmStepperPanel {

	AlgorithmStepperPanel(AlgorithmOptions options) {
		mOptions = options;
	}

	private void addButton(ViewGroup parent, String label, int iconId) {
		ButtonWidget w = mOptions.addButton(label, "icon", iconId,
				AbstractWidget.OPTION_DETACHED, true,
				AbstractWidget.OPTION_REFRESH_ALGORITHM, false);
		parent.addView(w.getView(), buildLayoutParams(true));
	}

	View view() {
		if (mView != null)
			return mView;
		LinearLayout layout = linearLayout(true);

		final int defaultTotalSteps = 500;

		final SliderWidget targetStepSlider = mOptions.addSlider(
				AlgorithmOptions.WIDGET_ID_TARGETSTEP, //
				AbstractWidget.OPTION_DETACHED, true, //
				AbstractWidget.OPTION_HAS_LABEL, false //
				);
		layout.addView(targetStepSlider.getView(), buildLayoutParams(true));

		// Add another detached widget to store the total steps
		AbstractWidget totalStepsSlider = mOptions.addSlider(
				AlgorithmOptions.WIDGET_ID_TOTALSTEPS,//
				"value", defaultTotalSteps,//
				AbstractWidget.OPTION_DETACHED, true);

		// Listen for changes to total steps, to change maximum value of target
		// step
		totalStepsSlider.addListener(new AbstractWidget.Listener() {
			@Override
			public void valueChanged(AbstractWidget widget) {
				targetStepSlider.setMaxValue(widget.getIntValue());
			}
		});

		LinearLayout v1 = linearLayout(false);
		layout.addView(v1, buildLayoutParams(false));
		{
			LinearLayout v2 = linearLayout(true);
			addButton(v2, ConcreteStepper.WIDGET_ID_JUMP_BWD, R.raw.jumpbwdicon);
			addButton(v2, ConcreteStepper.WIDGET_ID_JUMP_FWD, R.raw.jumpfwdicon);
			v1.addView(v2);
		}
		{
			LinearLayout v2 = linearLayout(true);
			addButton(v2, ConcreteStepper.WIDGET_ID_STEP_BWD, R.raw.stepbwdicon);
			addButton(v2, ConcreteStepper.WIDGET_ID_STEP_FWD, R.raw.stepfwdicon);
			v1.addView(v2);
		}

		mView = layout;
		return mView;
	}

	/**
	 * Build a LayoutParams for a horizontal component (wraps content
	 * horizontally, matches parent vertically)
	 * 
	 * @param fillRemaining
	 *            true if it's to fill the remaining horizontal space
	 */
	private static LinearLayout.LayoutParams buildLayoutParams(
			boolean fillRemaining) {
		LinearLayout.LayoutParams p = UITools.layoutParams(true);
		if (fillRemaining)
			p.weight = 1;
		return p;
	}

	/**
	 * Construct a LinearLayout
	 * 
	 * @param horizontal
	 *            true if its orientation is to be horizontal
	 */
	private LinearLayout linearLayout(boolean horizontal) {
		LinearLayout v = new LinearLayout(mOptions.getContext());
		v.setOrientation(horizontal ? LinearLayout.HORIZONTAL
				: LinearLayout.VERTICAL);
		return v;
	}

	private View mView;
	private AlgorithmOptions mOptions;
}
