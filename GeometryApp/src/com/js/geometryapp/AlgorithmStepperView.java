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

class AlgorithmStepperView {

	public static final String BUTTON_JUMP_BWD = "<<";
	public static final String BUTTON_JUMP_FWD = ">>";
	public static final String BUTTON_STEP_BWD = "<";
	public static final String BUTTON_STEP_FWD = ">";

	public AlgorithmStepperView(Context context, AlgorithmStepper stepper) {
		doNothing();
		mContext = context;
		mStepperController = stepper;
		mStepperController.setStepperView(this);
	}

	public void setTotalSteps(int totalSteps) {
		SliderWidget w = sOptions
				.getWidget(AlgorithmStepper.WIDGET_ID_TARGETSTEP);
		w.setMaxValue(totalSteps - 1);
	}

	private LinearLayout linearLayout(boolean horizontal) {
		LinearLayout v = new LinearLayout(mContext);
		v.setOrientation(horizontal ? LinearLayout.HORIZONTAL
				: LinearLayout.VERTICAL);
		return v;
	}

	private void addButton(ViewGroup parent, String label) {
		ButtonWidget w = sOptions.addButton(label, "detached", true);
		parent.addView(w.getView(), GeometryActivity.layoutParams(true, false));
	}

	protected View view() {
		if (mView == null) {
			sOptions = AlgorithmOptions.sharedInstance();
			LinearLayout layout = linearLayout(true);

			AbstractWidget w = sOptions.addSlider(
					AlgorithmStepper.WIDGET_ID_TARGETSTEP, //
					"detached", true, "withlabel", false, //
					"layout_vert", LayoutParams.MATCH_PARENT);

			layout.addView(w.getView(),
					GeometryActivity.layoutParams(true, true));

			LinearLayout v1 = linearLayout(false);
			layout.addView(v1, GeometryActivity.layoutParams(true, false));
			{
				LinearLayout v2 = linearLayout(true);
				addButton(v2, BUTTON_JUMP_BWD);
				addButton(v2, BUTTON_JUMP_FWD);
				v1.addView(v2);
			}
			{
				LinearLayout v2 = linearLayout(true);
				addButton(v2, BUTTON_STEP_BWD);
				addButton(v2, BUTTON_STEP_FWD);
				v1.addView(v2);
			}

			mView = layout;
		}
		return mView;
	}

	private View mView;
	private Context mContext;
	private AlgorithmStepper mStepperController;
	private AlgorithmOptions sOptions;
}
