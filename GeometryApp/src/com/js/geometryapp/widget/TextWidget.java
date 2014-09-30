package com.js.geometryapp.widget;

import java.util.Map;

import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextWidget extends AbstractWidget {

	public TextWidget(Context context, Map attributes) {
		super(context, attributes);
		attributes.put("hasvalue", false);

		mTextView = new TextView(context);
		mTextView.setText(this.getLabel(false));
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(intAttr(
				OPTION_LAYOUT_WIDTH, LayoutParams.MATCH_PARENT), intAttr(
				OPTION_LAYOUT_HEIGHT, LayoutParams.WRAP_CONTENT));
		getView().addView(mTextView, p);
	}

	private TextView mTextView;
}
