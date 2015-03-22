package com.js.gest;

import com.js.basic.Point;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import static com.js.basic.Tools.*;

class GestureEventFilter implements OnTouchListener {

  @Override
  public boolean onTouch(View view, MotionEvent event) {
    // Avoid Eclipse warnings:
    if (alwaysFalse())
      view.performClick();
    if (!mReceivingGesture
        && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      mReceivingGesture = true;
    }
    if (mReceivingGesture) {
      processGestureEvent((GesturePanel) view, event);
      if (event.getActionMasked() == MotionEvent.ACTION_UP) {
        mReceivingGesture = false;
      }
    }
    return true;
  }

  private void processGestureEvent(GesturePanel gesturePanel, MotionEvent event) {
    int actionMasked = event.getActionMasked();
    if (actionMasked == MotionEvent.ACTION_DOWN) {
      mStartEventTimeMillis = event.getEventTime();
      mTouchStrokeSet = new StrokeSet();
    }

    float eventTime = ((event.getEventTime() - mStartEventTimeMillis) / 1000.0f);

    int activeId = event.getPointerId(event.getActionIndex());
    MotionEvent.PointerCoords mCoord = new MotionEvent.PointerCoords();
    for (int i = 0; i < event.getPointerCount(); i++) {
      int ptrId = event.getPointerId(i);
      event.getPointerCoords(i, mCoord);
      Point pt = gesturePanel.flipVertically(new Point(mCoord.x, mCoord.y));
      mTouchStrokeSet.addPoint(eventTime, ptrId, pt);
    }

    GesturePanel.Listener listener = gesturePanel.getListener();
    listener.strokeSetExtended(mTouchStrokeSet);

    if (actionMasked == MotionEvent.ACTION_UP
        || actionMasked == MotionEvent.ACTION_POINTER_UP) {
      mTouchStrokeSet.stopStroke(activeId);
      if (!mTouchStrokeSet.areStrokesActive()) {
        mTouchStrokeSet.freeze();
        listener.strokeSetExtended(mTouchStrokeSet);
        gesturePanel.performMatch(mTouchStrokeSet);
      }
    }
  }

  // Stroke set from user touch event
  private StrokeSet mTouchStrokeSet;
  private long mStartEventTimeMillis;
  private boolean mReceivingGesture;
}
