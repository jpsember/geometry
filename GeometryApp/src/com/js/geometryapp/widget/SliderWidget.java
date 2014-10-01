package com.js.geometryapp.widget;

import java.util.Map;

import com.js.geometry.MyMath;
import com.js.geometryapp.AlgorithmOptions;

import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import static com.js.basic.Tools.*;

public class SliderWidget extends AbstractWidget {

	public SliderWidget(AlgorithmOptions options, Map attributes) {
		super(options, attributes);

		mSeekBar = new SeekBar(options.getContext());
		mSeekBar.setMax(maxValue() - minValue());
		int initialValue = intAttr(OPTION_VALUE, minValue());
		setValue(initialValue);

		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				setValue(Integer.toString(progress + minValue()));
			}
		});

		if (boolAttr(OPTION_HAS_LABEL, true))
			getView().addView(buildLabelView(true));

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, intAttr(OPTION_LAYOUT_HEIGHT,
						LayoutParams.WRAP_CONTENT));
		getView().addView(mSeekBar, p);
	}

	public void setMaxValue(int maxValue) {
		setAttribute("max", maxValue);
		int internalMax = maxValue() - minValue();
		mSeekBar.setMax(internalMax);
	}

	private int minValue() {
		return intAttr("min", 0);
	}

	private int maxValue() {
		return intAttr("max", 1000000);
	}

	@Override
	public void updateUserValue(String internalValue) {
		int sliderValue = Integer.parseInt(internalValue);
		int min = minValue();

		int max = maxValue();
		int userIntValue = MyMath.clamp(sliderValue, min, max);
		int progress = userIntValue - min;

		if (false) {
			warning("examining max progress");
			int maxProg = mSeekBar.getMax();
			if (progress > maxProg) {
				pr(getId() + ": attempt to set progress to " + progress
						+ " (internal value " + internalValue
						+ ") when max is " + maxProg);
			}
		}

		mSeekBar.setProgress(progress);
	}

	public void setValue(int progress) {
		setValue(Integer.toString(progress));
	}

	@Override
	public String parseUserValue() {
		int progress = mSeekBar.getProgress();
		int min = minValue();
		int userIntValue = progress + min;
		return Integer.toString(userIntValue);
	}

	private SeekBar mSeekBar;
}
