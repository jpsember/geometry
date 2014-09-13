package com.js.geometryapp;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
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
		mSeekBar.setMax(totalSteps - 1);
	}

	public void setTargetStep(int s) {
		mSeekBar.setProgress(s);
	}

	private LinearLayout linearLayout(boolean horizontal) {
		LinearLayout v = new LinearLayout(mContext);
		v.setOrientation(horizontal ? LinearLayout.HORIZONTAL
				: LinearLayout.VERTICAL);
		return v;
	}

	private void buildSeekBar() {
		SeekBar seekBar = new SeekBar(mContext);
		mSeekBar = seekBar;

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mStepperController.setTargetStep(progress);
			}
		});
	}

	protected View view() {
		if (mView == null) {
			LinearLayout layout = linearLayout(true);
			buildSeekBar();
			layout.addView(mSeekBar, GeometryActivity.layoutParams(true, true));

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

	private SeekBar mSeekBar;
	private View mView;
	private Context mContext;
	private AlgorithmStepper mStepperController;
}
