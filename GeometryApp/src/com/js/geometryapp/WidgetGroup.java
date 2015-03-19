package com.js.geometryapp;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;

import static com.js.android.UITools.*;
import static com.js.basic.Tools.*;

import com.js.geometryapp.widget.AbstractWidget;

class WidgetGroup {

  /**
   * Constructor; opens a vertical layout
   * 
   * @param useScrollView
   *          if true, wraps the (inner) LinearLayout within an (outer)
   *          containing ScrollView
   * 
   */
  public WidgetGroup(Context context, boolean useScrollView) {
    doNothing();
    setInnerContainer(linearLayout(context, true));
    if (useScrollView) {
      mScrollView = new ScrollView(context);
      mScrollView.setLayoutParams(new ScrollView.LayoutParams(
          LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
      mScrollView.addView(mContainer);
    } else {
      mContainer.setLayoutParams(layoutParams(false, 0));
    }
  }

  public LinearLayout getInnerContainer() {
    return mContainer;
  }

  /**
   * Get the outer container, which may be the same as the inner container, or
   * might be a ScrollView
   */
  public ViewGroup getOuterContainer() {
    if (mScrollView != null)
      return mScrollView;
    return mContainer;
  }

  public List<AbstractWidget> widgets() {
    return mWidgets;
  }

  public void add(AbstractWidget widget) {
    if (!widget.boolAttr(AbstractWidget.OPTION_HIDDEN, false)) {
      addView(widget.getView(), layoutParams(mContainer, 0));
    }
    mWidgets.add(widget);
  }

  public void addView(View view, LinearLayout.LayoutParams params) {
    mContainer.addView(view, params);
  }

  public void setInnerContainer(LinearLayout container) {
    mContainer = container;
  }

  private ScrollView mScrollView;
  private LinearLayout mContainer;
  private List<AbstractWidget> mWidgets = new ArrayList();
}
