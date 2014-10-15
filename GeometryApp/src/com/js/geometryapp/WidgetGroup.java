package com.js.geometryapp;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.js.android.UITools;
import com.js.geometryapp.widget.AbstractWidget;

class WidgetGroup {

	/**
	 * Constructor; opens a vertical layout
	 */
	public WidgetGroup(Context context) {
		setContainer(UITools.linearLayout(context, true));
	}

	public ViewGroup container() {
		return mContainer;
	}

	public List<AbstractWidget> widgets() {
		return mWidgets;
	}

	public void add(AbstractWidget widget) {
		if (!widget.boolAttr(AbstractWidget.OPTION_HIDDEN, false)) {
			addView(widget.getView());
		}
		mWidgets.add(widget);
	}

	public LayoutParams layoutParamsForCurrentContainer() {
		return UITools.layoutParams(mContainer);
	}

	public void addView(View view) {
		addView(view, layoutParamsForCurrentContainer());
	}

	public void addView(View view, LinearLayout.LayoutParams params) {
		mContainer.addView(view, params);
	}

	public void setContainer(LinearLayout container) {
		mContainer = container;
	}

	public LinearLayout getContainer() {
		return mContainer;
	}

	private LinearLayout mContainer;
	private List<AbstractWidget> mWidgets = new ArrayList();
}
