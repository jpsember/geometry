package com.js.geometryapp;

import com.js.geometry.MyMath;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class AlgorithmStepperView {

	private static final String BUTTON_JUMP_BWD = "<<";
	private static final String BUTTON_JUMP_FWD = ">>";
	private static final String BUTTON_STEP_BWD = "<";
	private static final String BUTTON_STEP_FWD = ">";

	public AlgorithmStepperView(Context context) {
		mContext = context;
		setTotalSteps(100);
	}

	public void setTotalSteps(int totalSteps) {
		mTotalSteps = totalSteps;
	}

	private LinearLayout linearLayout(boolean horizontal) {
		LinearLayout v = new LinearLayout(mContext);
		v.setOrientation(horizontal ? LinearLayout.HORIZONTAL
				: LinearLayout.VERTICAL);
		return v;
	}

	public int totalSteps() {
		return mTotalSteps;
	}

	private void buildSeekBar() {
		SeekBar seekBar = new SeekBar(mContext);
		mSeekBar = seekBar;
		seekBar.setMax(totalSteps());

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mCurrentStep = progress;
			}
		});
	}

	public View view() {
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
		int delta = 0;
		if (label == BUTTON_STEP_FWD)
			delta = 1;
		else if (label == BUTTON_STEP_BWD)
			delta = -1;
		int val = currentStep() + delta;
		val = MyMath.clamp(val, 0, totalSteps() - 1);
		mSeekBar.setProgress(val);
	}

	public int currentStep() {
		return mCurrentStep;
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

	private int mCurrentStep;
	private SeekBar mSeekBar;
	private View mView;
	private Context mContext;
	private int mTotalSteps;
}
