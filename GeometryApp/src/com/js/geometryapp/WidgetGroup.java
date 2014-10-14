package com.js.geometryapp;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.js.android.UITools;
import com.js.geometryapp.widget.AbstractWidget;

class WidgetGroup {

	/**
	 * Constructor; opens a vertical layout
	 */
	public WidgetGroup(Context context) {
		mContext = context;
		setContainer(constructContainer(true), false);
	}

	public ViewGroup view() {
		return mView;
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

	public void addView(View view) {
		addView(view, UITools.layoutParams(activeContainerIsVertical()));
	}

	public void addView(View view, LinearLayout.LayoutParams params) {
		mView.addView(view, params);
	}


	LinearLayout getContainer() {
		return mView;
	}

	private boolean activeContainerIsVertical() {
		return mView.getOrientation() == LinearLayout.VERTICAL;
	}

	void setContainer(LinearLayout container, boolean addToExisting) {
		if (addToExisting)
			mView.addView(container,
					UITools.layoutParams(activeContainerIsVertical()));
		mView = container;
	}

	/**
	 * Construct a view for stacking widgets horizontally or vertically
	 */
	LinearLayout constructContainer(boolean vertical) {
		LinearLayout view = new LinearLayout(mContext);
		view.setOrientation(vertical ? LinearLayout.VERTICAL
				: LinearLayout.HORIZONTAL);
		UITools.applyDebugColors(view);
		return view;
	}

	private Context mContext;
	private LinearLayout mView;
	private List<AbstractWidget> mWidgets = new ArrayList();
}
