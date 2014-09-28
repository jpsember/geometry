package com.js.geometryapp.widget;

import java.util.Map;

import com.js.geometryapp.AlgorithmOptions;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

public class ButtonWidget extends AbstractWidget {

	public static final Factory FACTORY = new AbstractWidget.Factory() {
		@Override
		public String getName() {
			doNothing();
			return "button";
		}

		@Override
		public AbstractWidget constructInstance(Context context, Map attributes) {
			return new ButtonWidget(context, attributes);
		}
	};

	public ButtonWidget(Context context, Map attributes) {
		super(context, attributes);
		attributes.put("hasvalue", false);

		if (!isHidden()) {
			mButton = new Button(context);
			mButton.setText(getLabel(false));
			mButton.setOnClickListener(new Button.OnClickListener() {

				@Override
				public void onClick(View v) {
					AlgorithmOptions.sharedInstance().processWidgetValue(
							ButtonWidget.this, mListeners);
				}
			});

			LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			mButton.setLayoutParams(buttonParams);

			getView().addView(mButton);
		}
	}

	private Button mButton;
}
