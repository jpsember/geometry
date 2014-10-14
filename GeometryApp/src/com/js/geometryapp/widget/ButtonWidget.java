package com.js.geometryapp.widget;

import java.util.Map;

import com.js.geometryapp.AlgorithmOptions;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class ButtonWidget extends AbstractWidget {

	public ButtonWidget(AlgorithmOptions options, Map attributes) {
		super(options, attributes);
		attributes.put("hasvalue", false);

		mButton = new Button(options.getContext());
		mButton.setText(getLabel(false));
		mButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyListeners();
			}
		});

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(intAttr(
				OPTION_LAYOUT_WIDTH, LayoutParams.MATCH_PARENT), intAttr(
				OPTION_LAYOUT_HEIGHT, LayoutParams.WRAP_CONTENT));
		getView().addView(mButton, p);
	}

	@Override
	public void setEnabled(boolean enabled) {
		mButton.setEnabled(enabled);
	}

	private Button mButton;
}
