package com.js.geometryapp;

import java.util.Map;

import com.js.geometry.MyMath;

import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import static com.js.basic.Tools.*;

public class SliderWidget extends AbstractWidget {

	public static final Factory FACTORY = new AbstractWidget.Factory() {
		@Override
		public String getName() {
			doNothing();
			return "slider";
		}

		@Override
		public AbstractWidget constructInstance(Context context, Map attributes) {
			return new SliderWidget(context, attributes);
		}
	};

	public SliderWidget(Context context, Map attributes) {
		super(context, attributes);

		mSeekBar = new SeekBar(context);
		mSeekBar.setMax(maxValue() - minValue());

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
		getView().addView(buildLabelView(true));

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		getView().addView(mSeekBar, p);
	}

	private int minValue() {
		return intAttr("min", 0);
	}

	private int maxValue() {
		return intAttr("max", 1000000);
	}

	public void updateUserValue(String internalValue) {
		int sliderValue = Integer.parseInt(internalValue);
		int min = minValue();
		int max = maxValue();
		sliderValue = MyMath.clamp(sliderValue, min, max);
		mSeekBar.setProgress(sliderValue - min);
	}

	public void setValue(int progress) {
		setValue(Integer.toString(progress));
	}

	/**
	 * Get displayed value, and transform to 'internal' representation.
	 */
	public String parseUserValue() {
		int progress = mSeekBar.getProgress();
		int min = minValue();
		int sliderPos = progress + min;
		return Integer.toString(sliderPos);
	}

	private SeekBar mSeekBar;
}
