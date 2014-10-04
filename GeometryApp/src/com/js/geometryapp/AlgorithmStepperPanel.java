package com.js.geometryapp;

import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.ButtonWidget;
import com.js.geometryapp.widget.SliderWidget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

/**
 * View that contains the step controls (slider and page / step buttons)
 */
class AlgorithmStepperPanel {

	/**
	 * Factory constructor
	 * 
	 * @param context
	 * @return View containing the various controls
	 */
	public static View build(AlgorithmOptions options) {
		AlgorithmStepperPanel v = new AlgorithmStepperPanel(options);
		return v.view();
	}

	AlgorithmStepperPanel(AlgorithmOptions options) {
		doNothing();
		mOptions = options;
	}

	private void addButton(ViewGroup parent, String label) {
		ButtonWidget w = mOptions.addButton(label,
				AbstractWidget.OPTION_DETACHED, true,
				AbstractWidget.OPTION_REFRESH_ALGORITHM, false);
		parent.addView(w.getView(), buildLayoutParams(true));
	}

	private View view() {
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
			addButton(v2, ConcreteStepper.WIDGET_ID_JUMP_BWD);
			addButton(v2, ConcreteStepper.WIDGET_ID_JUMP_FWD);
			v1.addView(v2);
		}
		{
			LinearLayout v2 = linearLayout(true);
			addButton(v2, ConcreteStepper.WIDGET_ID_STEP_BWD);
			addButton(v2, ConcreteStepper.WIDGET_ID_STEP_FWD);
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
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		p.weight = fillRemaining ? 1 : 0;
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
