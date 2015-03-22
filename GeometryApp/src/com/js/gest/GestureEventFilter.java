package com.js.gest;

import java.util.ArrayList;

import com.js.basic.Point;
import com.js.gest.GestureSet.Match;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import static com.js.basic.Tools.*;

public class GestureEventFilter implements OnTouchListener {

  GestureEventFilter(GesturePanel panel) {
    mGesturePanel = panel;
  }

  public void setListener(Listener listener) {
    mListener = listener;
  }

  public void setGestures(GestureSet c) {
    mStrokeSetCollection = c;
  }

  private GesturePanel gesturePanel() {
    return mGesturePanel;
  }

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
      processGestureEvent(event);
      if (event.getActionMasked() == MotionEvent.ACTION_UP) {
        mReceivingGesture = false;
      }
    }
    return true;
  }

  private void processGestureEvent(MotionEvent event) {
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
      Point pt = mGesturePanel.flipVertically(new Point(mCoord.x, mCoord.y));
      mTouchStrokeSet.addPoint(eventTime, ptrId, pt);
    }

    if (mListener == null)
      warning("no listener defined");
    else
      mListener.strokeSetExtended(mTouchStrokeSet);

    if (actionMasked == MotionEvent.ACTION_UP
        || actionMasked == MotionEvent.ACTION_POINTER_UP) {
      mTouchStrokeSet.stopStroke(activeId);
      if (!mTouchStrokeSet.areStrokesActive()) {
        mTouchStrokeSet.freeze();
        if (mListener != null) {
          mListener.strokeSetExtended(mTouchStrokeSet);
          if (mStrokeSetCollection == null)
            warning("no stroke collection defined");
          else
            performMatch();
        }
      }
    }
  }

  private void performMatch() {
    if (mTouchStrokeSet.isTap()) {
      mListener.processGesture(GestureSet.GESTURE_TAP);
      return;
    }

    mMatch = null;
    StrokeSet set = mTouchStrokeSet;
    set = set.fitToRect(null);
    set = set.normalize();

    ArrayList<GestureSet.Match> matches = new ArrayList();
    Match match = mStrokeSetCollection.findMatch(set, null, matches);
    if (match == null)
      return;
    // If the match cost is significantly less than the second best, use it
    if (matches.size() >= 2) {
      Match match2 = matches.get(1);
      if (match.cost() * 1.5f > match2.cost())
        return;
    }
    mMatch = match;
    mListener.processGesture(mMatch.strokeSet().aliasName());
    setDisplayedGesture(mMatch.strokeSet().name(), true);
  }

  /**
   * Set which gesture is displayed; does nothing if in shared view mode
   * 
   * @param gestureName
   *          name to display, or null; if no such gesture exists, removes any
   *          existing gesture
   * @param substituteAlias
   *          if true, and named gesture has an alias, displays the alias
   *          instead
   */
  public void setDisplayedGesture(String gestureName, boolean substituteAlias) {
    StrokeSet set = null;
    if (gestureName != null) {
      set = mStrokeSetCollection.get(gestureName);
      if (set != null) {
        if (substituteAlias && set.hasAlias()) {
          set = mStrokeSetCollection.get(set.aliasName());
        }
      }
    }
    gesturePanel().setGesture(set);
  }

  public static interface Listener {
    /**
     * For development purposes only: called when the gesture being constructed
     * by the user has been changed. If it is frozen, it is complete
     */
    void strokeSetExtended(StrokeSet strokeSet);

    /**
     * In normal use, this is the only method that has to do anything; the
     * client should handle the recognized gesture
     */
    void processGesture(String gestureName);
  }

  // Stroke set from user touch event
  private StrokeSet mTouchStrokeSet;
  private Listener mListener;
  private long mStartEventTimeMillis;
  private GestureSet mStrokeSetCollection;
  private Match mMatch;
  private GesturePanel mGesturePanel;
  private boolean mReceivingGesture;

}
