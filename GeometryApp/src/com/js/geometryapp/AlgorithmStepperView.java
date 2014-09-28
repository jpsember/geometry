package com.js.geometryapp;

import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.SliderWidget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

class AlgorithmStepperView {

	private static final String BUTTON_JUMP_BWD = "<<";
	private static final String BUTTON_JUMP_FWD = ">>";
	private static final String BUTTON_STEP_BWD = "<";
	private static final String BUTTON_STEP_FWD = ">";

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
				v2.addView(button(BUTTON_JUMP_BWD));
				v2.addView(button(BUTTON_JUMP_FWD));
				v1.addView(v2);
			}
			{
				LinearLayout v2 = linearLayout(true);
				v2.addView(button(BUTTON_STEP_BWD));
				v2.addView(button(BUTTON_STEP_FWD));
				v1.addView(v2);
			}

			mView = layout;
		}
		return mView;
	}

	private void processButtonPress(String label) {
		mStepperController.verifyDelegateDefined();

		if (label == BUTTON_STEP_FWD)
			mStepperController.adjustTargetStep(1);
		else if (label == BUTTON_STEP_BWD)
			mStepperController.adjustTargetStep(-1);
		else if (label == BUTTON_JUMP_FWD)
			mStepperController.adjustDisplayedMilestone(1);
		else if (label == BUTTON_JUMP_BWD)
			mStepperController.adjustDisplayedMilestone(-1);
	}

	private Button button(final String label) {
		Button b = new Button(mContext);
		b.setText(label);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				processButtonPress(label);
			}
		});
		return b;
	}

	private View mView;
	private Context mContext;
	private AlgorithmStepper mStepperController;
	private AlgorithmOptions sOptions;
}
