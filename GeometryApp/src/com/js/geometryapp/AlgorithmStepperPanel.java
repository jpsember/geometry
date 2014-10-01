package com.js.geometryapp;

import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.ButtonWidget;
import com.js.geometryapp.widget.SliderWidget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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
	public static View build(Context context) {
		AlgorithmStepperPanel v = new AlgorithmStepperPanel(context);
		return v.view();
	}

	private AlgorithmStepperPanel(Context context) {
		doNothing();
		mContext = context;
	}

	private LinearLayout linearLayout(boolean horizontal) {
		LinearLayout v = new LinearLayout(mContext);
		v.setOrientation(horizontal ? LinearLayout.HORIZONTAL
				: LinearLayout.VERTICAL);
		return v;
	}

	private void addButton(ViewGroup parent, String label) {
		ButtonWidget w = sOptions.addButton(label,
				AbstractWidget.OPTION_DETACHED, true,
				AbstractWidget.OPTION_REFRESH_ALGORITHM, false,
				AbstractWidget.OPTION_PADDING, false);
		parent.addView(w.getView(), GeometryActivity.layoutParams(true, false));
	}

	private View view() {
		if (mView != null)
			return mView;
		sOptions = AlgorithmOptions.sharedInstance();
		LinearLayout layout = linearLayout(true);

		final int defaultTotalSteps = 500;

		final SliderWidget targetStepSlider = sOptions.addSlider(
				AlgorithmOptions.WIDGET_ID_TARGETSTEP, //
				AbstractWidget.OPTION_DETACHED, true, //
				AbstractWidget.OPTION_HAS_LABEL, false, //
				AbstractWidget.OPTION_PADDING, false, //
				AbstractWidget.OPTION_LAYOUT_HEIGHT, LayoutParams.MATCH_PARENT);

		layout.addView(targetStepSlider.getView(),
				GeometryActivity.layoutParams(true, true));

		// Add another detached widget to store the total steps
		AbstractWidget totalStepsSlider = sOptions.addSlider(
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
		layout.addView(v1, GeometryActivity.layoutParams(true, false));
		{
			LinearLayout v2 = linearLayout(true);
			addButton(v2, AlgorithmStepper.WIDGET_ID_JUMP_BWD);
			addButton(v2, AlgorithmStepper.WIDGET_ID_JUMP_FWD);
			v1.addView(v2);
		}
		{
			LinearLayout v2 = linearLayout(true);
			addButton(v2, AlgorithmStepper.WIDGET_ID_STEP_BWD);
			addButton(v2, AlgorithmStepper.WIDGET_ID_STEP_FWD);
			v1.addView(v2);
		}

		mView = layout;
		return mView;
	}

	private View mView;
	private Context mContext;
	private AlgorithmOptions sOptions;
}
