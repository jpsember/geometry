package com.js.geometryapp.widget;

import java.util.Map;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextWidget extends AbstractWidget {

	/**
	 * If true, text is drawn with a larger font
	 */
	public static final String OPTION_HEADER = "header";

	/**
	 * If true, text is drawn centered horizontally
	 */
	public static final String OPTION_CENTER = "center";

	public TextWidget(Context context, Map attributes) {
		super(context, attributes);
		attributes.put("hasvalue", false);

		mTextView = new TextView(context);
		mTextView.setText(this.getLabel(false));

		if (boolAttr("header", false)) {
			mTextView.setTextSize(mTextView.getTextSize() * 1.3f);
		}
		if (boolAttr("center", false)) {
			mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		}

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(intAttr(
				OPTION_LAYOUT_WIDTH, LayoutParams.MATCH_PARENT), intAttr(
				OPTION_LAYOUT_HEIGHT, LayoutParams.WRAP_CONTENT));
		getView().addView(mTextView, p);
	}

	private TextView mTextView;
}
