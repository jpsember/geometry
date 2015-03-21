package com.js.gest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import com.js.android.MyTouchListener;
import com.js.basic.Point;
import com.js.gest.GestureSet.Match;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import static com.js.basic.Tools.*;
import static com.js.android.UITools.*;

public class GestureEventFilter extends MyTouchListener {

  private static final int STATE_UNATTACHED = 0;
  private static final int STATE_DORMANT = 1;
  private static final int STATE_BUFFERING = 2;
  private static final int STATE_RECORDING = 3;
  private static final int STATE_FORWARDING = 4;
  private static final int STATE_IGNORING = 5;
  private static final int STATE_STOPPED = 6;

  public GestureEventFilter() {
    // Enable this line to print diagnostic information:
    // mTraceActive = true;
  }

  /**
   * Construct a view to be used for displaying the gesture panel. Also calls
   * setView() to make the view this touch listener's view
   */
  public View constructGesturePanel(Context context) {
    if (mConstructedView)
      throw new IllegalStateException();
    View view = new GesturePanel(context);
    mConstructedView = true;
    setView(view);
    return view;
  }

  public void setListener(Listener listener) {
    mListener = listener;
  }

  public void setGestures(GestureSet c) {
    mStrokeSetCollection = c;
  }

  @Override
  public void prependTo(MyTouchListener listener) {
    unimp("eliminate UNATTACHED state and this method");
    if (state() != STATE_UNATTACHED)
      throw new IllegalStateException();
    super.prependTo(listener);
    setState(STATE_DORMANT);
  }

  @Override
  public void setView(View view) {
    if (!(view instanceof GesturePanel))
      throw new IllegalArgumentException("must be GesturePanel");
    super.setView(view);
    setState(STATE_DORMANT);
  }

  public void detach() {
    if (state() == STATE_STOPPED)
      return;
    setState(STATE_STOPPED);
    unimp("remove listener from view?");
    // this.remove();
    // mView.setOnTouchListener(null);
    mListener = null;
  }

  private void trace(Object message) {
    if (mTraceActive)
      pr(message);
  }

  private final static String[] sStateNames = { "UNATTACHED", "DORMANT",
      "BUFFERING", "RECORDING", "FORWARDING", "IGNORING", "STOPPED" };

  private static String stateName(int state) {
    return sStateNames[state];
  }

  private int state() {
    return mState;
  }

  private void setState(int s) {
    trace("Set state from " + stateName(mState) + " to " + stateName(s));
    mState = s;
  }

  /**
   * Push a copy of an event onto our queue for delayed processing
   * 
   * @param event
   * @return the copy that was pushed a copy of the event
   */
  private MotionEvent bufferEvent(MotionEvent event) {
    MotionEvent eventCopy = MotionEvent.obtain(event);
    mEventQueue.add(eventCopy);
    return eventCopy;
  }

  /**
   * Send any previously buffered events to the view
   */
  private void flushBufferedEvents() {
    // Set flag instructing our filter to pass the event through to the view's
    // original handler
    mPassingEventFlag = true;
    if (mEventQueue.size() > 1)
      trace("    flushing " + mEventQueue.size() + " buffered events");
    while (true) {
      MotionEvent event = mEventQueue.poll();
      if (event == null)
        break;
      getView().dispatchTouchEvent(event);
      event.recycle();
    }
    mPassingEventFlag = false;
  }

  /**
   * Send any previously buffered events to the gesture recording logic
   */
  private void flushGestureEvents() {
    if (mEventQueue.size() > 1)
      trace("    processing " + mEventQueue.size() + " gesture events");
    while (true) {
      MotionEvent event = mEventQueue.poll();
      if (event == null)
        break;
      processGestureEvent(event);
      event.recycle();
    }
  }

  private void disposeBufferedEvents() {
    while (true) {
      MotionEvent event = mEventQueue.poll();
      if (event == null)
        break;
      event.recycle();
    }
  }

  private void processDormantState(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      setState(STATE_BUFFERING);
      processBufferingState(event);
    } else {
      flushBufferedEvents();
    }
  }

  private GesturePanel gesturePanel() {
    return (GesturePanel) getView();
  }

  private void processBufferingState(MotionEvent event) {

    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      setState(STATE_RECORDING);
      processRecordingState(event);
      return;
    }

    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      setState(STATE_FORWARDING);
      processForwardingState(event);
    } else {
      bufferEvent(event);
    }
  }

  private void processRecordingState(MotionEvent event) {
    event = bufferEvent(event);
    flushGestureEvents();
    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      setState(STATE_DORMANT);
    }
  }

  private void processForwardingState(MotionEvent event) {
    event = bufferEvent(event);
    flushBufferedEvents();
    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      setState(STATE_DORMANT);
    }
  }

  private void processIgnoringState(MotionEvent event) {
    disposeBufferedEvents();
    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      setState(STATE_DORMANT);
    }
  }

  private void processStoppedState(MotionEvent event) {
    bufferEvent(event);
    flushBufferedEvents();
  }

  private boolean onTouchAux(MotionEvent event) {
    switch (state()) {
    case STATE_DORMANT:
      processDormantState(event);
      break;
    case STATE_BUFFERING:
      processBufferingState(event);
      break;
    case STATE_RECORDING:
      processRecordingState(event);
      break;
    case STATE_FORWARDING:
      processForwardingState(event);
      break;
    case STATE_IGNORING:
      processIgnoringState(event);
      break;
    case STATE_STOPPED:
      processStoppedState(event);
      break;
    }
    return true;
  }

  @Override
  public boolean onTouch(MotionEvent event) {
    trace("GestureEventFilter, onTouch event " + dump(event)
        + ", passing events: " + d(mPassingEventFlag));
    if (event.getActionMasked() != MotionEvent.ACTION_MOVE)
      trace("onTouch: " + dump(event) + " state " + stateName(state()));

    // If we're forwarding events to the original handler, do so
    if (mPassingEventFlag) {
      return false;
    }

    return onTouchAux(event);
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
      Point pt = new Point(mCoord.x, mCoord.y);
      pt.y = getView().getHeight() - mCoord.y;
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

  private boolean mTraceActive;
  private Deque<MotionEvent> mEventQueue = new ArrayDeque();
  private boolean mPassingEventFlag;
  private int mState;
  private GestureSet mStrokeSetCollection;
  private Match mMatch;
  private boolean mConstructedView;
}
