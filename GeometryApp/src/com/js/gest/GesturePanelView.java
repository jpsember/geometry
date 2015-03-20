package com.js.gest;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

/**
 * View subclass to serve as the view associated with a GestureEventFilter in
 * OWNVIEW mode
 */
class GesturePanelView extends View {

  public GesturePanelView(Context context, GestureEventFilter filter) {
    super(context);
    mFilter = filter;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    mFilter.draw(canvas);
  }

  private GestureEventFilter mFilter;
}
