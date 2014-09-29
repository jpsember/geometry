package com.js.geometryapp.widget;

import java.util.Map;

import com.js.geometryapp.AlgorithmOptions;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class ButtonWidget extends AbstractWidget {

	public ButtonWidget(Context context, Map attributes) {
		super(context, attributes);
		attributes.put("hasvalue", false);

		mButton = new Button(context);
		mButton.setText(getLabel(false));
		mButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				AlgorithmOptions.sharedInstance().processWidgetValue(
						ButtonWidget.this, mListeners);
			}
		});

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(intAttr(
				OPTION_LAYOUT_WIDTH, LayoutParams.MATCH_PARENT), intAttr(
				OPTION_LAYOUT_HEIGHT, LayoutParams.WRAP_CONTENT));
		getView().addView(mButton, p);
	}

	private Button mButton;
}
