package com.js.gest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import com.js.android.MyTouchListener;
import com.js.android.UITools;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.basic.Tools;
import com.js.gest.GestureSet.Match;

import android.os.Handler;
import android.view.MotionEvent;
import static com.js.basic.Tools.*;

public class GestureEventFilter extends MyTouchListener {

  // User must move by a certain distance within this time in order to switch
  // from BUFFERING to RECORDING (instead of to FORWARDING)
  private static final int BUFFERING_TIME_MS = 100;
  // Minimum distance finger must move by end of buffering time to be
  // interpreted as a gesture
  private static final float MIN_GESTURE_DISTANCE = .5f * BUFFERING_TIME_MS;

  private static final int STATE_UNATTACHED = 0;
  private static final int STATE_DORMANT = 1;
  private static final int STATE_BUFFERING = 2;
  private static final int STATE_RECORDING = 3;
  private static final int STATE_FORWARDING = 4;
  private static final int STATE_STOPPED = 5;

  public GestureEventFilter() {
    // mTraceActive = true;
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

  public void detach() {
    if (state() == STATE_STOPPED)
      return;
    setState(STATE_STOPPED);
    unimp("remove listener from view?");
    // this.remove();
    // mView.setOnTouchListener(null);
    mListener = null;
  }

  private void pr(Object message) {
    if (mTraceActive)
      Tools.pr(message);
  }

  private final static String[] sStateNames = { "UNATTACHED", "DORMANT",
      "BUFFERING", "RECORDING", "FORWARDING", "STOPPED" };

  private static String stateName(int state) {
    return sStateNames[state];
  }

  private int state() {
    return mState;
  }

  private void setState(int s) {
    pr("Set state from " + stateName(mState) + " to " + stateName(s));
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
      pr("    flushing " + mEventQueue.size() + " buffered events");
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
      pr("    processing " + mEventQueue.size() + " gesture events");
    while (true) {
      MotionEvent event = mEventQueue.poll();
      if (event == null)
        break;
      processGestureEvent(event);
      event.recycle();
    }
  }

  private void processDormantState(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      // Post an event to switch to FORWARDING automatically in case user
      // doesn't trigger any further events for a while
      postForwardTestEvent();
      setState(STATE_BUFFERING);
      processBufferingState(event);
    } else {
      flushBufferedEvents();
    }
  }

  /**
   * Post an event to switch to FORWARDING automatically in case user doesn't
   * trigger any further events for a while, to give the state machine a chance
   * to make the RECORDING vs FORWARDING decision
   */
  private void postForwardTestEvent() {
    sHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        pr("Timer task fired, state= " + stateName(state()));
        if (state() == STATE_BUFFERING) {
          setState(STATE_FORWARDING);
          flushBufferedEvents();
        }
      }
    }, (long) (BUFFERING_TIME_MS * 1.5));
  }

  private void processBufferingState(MotionEvent event) {

    MotionEvent startEvent = mEventQueue.peek();
    // Has enough time elapsed since start event to make a decision about
    // whether it's a gesture event?
    long elapsed = 0;
    if (startEvent != null)
      elapsed = event.getEventTime() - startEvent.getEventTime();
    if (elapsed > BUFFERING_TIME_MS) {
      Point pt0 = new Point(startEvent.getRawX(), startEvent.getRawY());
      Point pt = new Point(event.getRawX(), event.getRawY());
      float distance = MyMath.distanceBetween(pt0, pt);
      pr("  ...distance=" + d(distance));
      unimp("use a distance that's proportional to the device density");
      if (distance > MIN_GESTURE_DISTANCE) {
        setState(STATE_RECORDING);
        processRecordingState(event);
      } else {
        setState(STATE_FORWARDING);
        processForwardingState(event);
      }
    } else {
      // pr("  ...not enough elapsed time, continuing to buffer");
      if (event.getActionMasked() == MotionEvent.ACTION_UP) {
        setState(STATE_FORWARDING);
        processForwardingState(event);
      } else {
        bufferEvent(event);
      }
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
    case STATE_STOPPED:
      processStoppedState(event);
      break;
    }
    return true;
  }

  @Override
  public boolean onTouch(MotionEvent event) {
    pr("GestureEventFilter, onTouch event " + UITools.dump(event)
        + ", passing events: " + d(mPassingEventFlag));
    if (event.getActionMasked() != MotionEvent.ACTION_MOVE)
      pr("onTouch: " + UITools.dump(event) + " state " + stateName(state()));

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
    mMatch = null;
    StrokeSet set = mTouchStrokeSet;
    set = set.fitToRect(null);
    set = set.normalize();

    ArrayList<GestureSet.Match> matches = new ArrayList();
    Match match = mStrokeSetCollection.findMatch(set, null, matches);
    do {
      if (match == null)
        break;
      if (match.cost() >= 0.15f)
        break;
      mMatch = match;
      mListener.processGesture(mMatch.strokeSet().aliasName());
    } while (false);
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

  private static Handler sHandler = new Handler();
  private boolean mTraceActive;
  private Queue<MotionEvent> mEventQueue = new ArrayDeque();
  private boolean mPassingEventFlag;
  private int mState;
  private GestureSet mStrokeSetCollection;
  private Match mMatch;
}
