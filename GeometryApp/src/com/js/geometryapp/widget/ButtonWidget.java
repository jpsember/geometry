package com.js.geometryapp.widget;

import java.util.Map;

import com.js.geometryapp.AlgorithmOptions;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class ButtonWidget extends AbstractWidget {

	public ButtonWidget(AlgorithmOptions options, Map attributes) {
		super(options, attributes);
		attributes.put("hasvalue", false);

		int iconId = intAttr("icon", -1);
		if (iconId < 0) {
			Button b = new Button(options.getContext());
			b.setText(getLabel(false));
			b.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					notifyListeners();
				}
			});

			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
					intAttr(OPTION_LAYOUT_WIDTH, LayoutParams.MATCH_PARENT),
					intAttr(OPTION_LAYOUT_HEIGHT, LayoutParams.WRAP_CONTENT));
			getView().addView(b, p);
			mButton = b;
		} else {
			ImageButton b;
			b = new ImageButton(options.getContext());
			b.setImageResource(iconId);
			b.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					notifyListeners();
				}
			});

			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
					intAttr(OPTION_LAYOUT_WIDTH, LayoutParams.WRAP_CONTENT),
					intAttr(OPTION_LAYOUT_HEIGHT, LayoutParams.WRAP_CONTENT));
			getView().addView(b, p);
			mImageButton = b;

		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (isIcon())
			mImageButton.setEnabled(enabled);
		else
			mButton.setEnabled(enabled);
	}

	private boolean isIcon() {
		return mImageButton != null;
	}

	private Button mButton;
	private ImageButton mImageButton;
}
