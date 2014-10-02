package com.js.geometryapp;

import java.util.ArrayList;

import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

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
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			mView.addView(widget.getView(), p);
		}
		mWidgets.add(widget);
	}

	private ViewGroup mView;
	private ArrayList<AbstractWidget> mWidgets;
}
