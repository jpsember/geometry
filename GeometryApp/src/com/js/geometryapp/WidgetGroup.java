package com.js.geometryapp;

import java.util.ArrayList;

import android.view.ViewGroup;

import com.js.android.UITools;
import com.js.geometryapp.widget.AbstractWidget;

class WidgetGroup {
	public WidgetGroup(ViewGroup view) {
		mView = view;
		mWidgets = new ArrayList();
	}

	public ViewGroup view() {
		return mView;
	}

	public ArrayList<AbstractWidget> widgets() {
		return mWidgets;
	}

	public void add(AbstractWidget widget) {
		if (!widget.boolAttr(AbstractWidget.OPTION_HIDDEN, false)) {
			mView.addView(widget.getView(), UITools.layoutParams(false));
		}
		mWidgets.add(widget);
	}

	private ViewGroup mView;
	private ArrayList<AbstractWidget> mWidgets;
}
