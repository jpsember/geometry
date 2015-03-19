package com.js.gest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;

import com.js.android.MyActivity;
import com.js.android.MyTouchListener;
import com.js.android.UITools;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.gest.GestureSet.Match;

import android.graphics.Canvas;
import android.os.Handler;
import android.view.MotionEvent;
import static com.js.basic.Tools.*;

public class GestureEventFilter extends MyTouchListener {

  // User must move by a certain distance within this time in order to switch
  // from BUFFERING to RECORDING (instead of to FORWARDING)
  private static final int BUFFERING_TIME_MS = 200;

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

    // Enable this line to display gesture vs. non-gesture decision:
    // mTracker = new DecisionTracker();
  }

  /**
   * Have event filter operate in 'floating view' mode. A small translucent
   * rectangle is drawn in the view, and gestures must be started within it
   */
  public void setFloatingViewMode() {
    if (floatingViewMode())
      throw new IllegalStateException();
    mFloatingViewMode = true;
  }

  private GesturePanel floatingPanel() {
    if (!floatingViewMode())
      throw new IllegalStateException();
    if (mGesturePanel == null) {
      mGesturePanel = new GesturePanel(getView());
    }
    return mGesturePanel;
  }

  public void draw(Canvas canvas) {
    if (!floatingViewMode())
      return;
    floatingPanel().draw(canvas);
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

  public boolean floatingViewMode() {
    return mFloatingViewMode;
  }

  private void processDormantState(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      if (!floatingViewMode()) {
        // Post an event to switch to FORWARDING automatically in case user
        // doesn't trigger any further events for a while
        postForwardTestEvent();
      }
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
        trace("Timer task fired, state= " + stateName(state()));
        if (state() == STATE_BUFFERING) {
          setState(STATE_FORWARDING);
          flushBufferedEvents();
        }
      }
    }, (long) (BUFFERING_TIME_MS * 1.5));
  }

  private static Point rawLocation(MotionEvent event) {
    return new Point(event.getRawX(), event.getRawY());
  }

  private static float elapsedTime(MotionEvent event1, MotionEvent event2) {
    return (event2.getEventTime() - event1.getEventTime()) / 1000.0f;
  }

  private void processBufferingState(MotionEvent event) {

    if (mTracker != null)
      mTracker.addEvent(event);

    if (floatingViewMode()) {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        Point touchLoc = new Point(event.getX(), event.getY());
        if (floatingPanel().containsPoint(touchLoc)) {

          // If panel is minimized, maximize it, and ignore the rest of this
          // touch sequence
          if (floatingPanel().isMinimized()) {
            floatingPanel().setMinimized(false);
            setState(STATE_IGNORING);
          } else {
            setState(STATE_RECORDING);
            processRecordingState(event);
          }
        } else {
          setState(STATE_FORWARDING);
          processForwardingState(event);
        }
        return;
      }
    } else {

      // Attempt to decide whether it's a gesture or not
      final int MIN_STEPS = 3;
      if (mEventQueue.size() >= 1 + MIN_STEPS) {
        Iterator<MotionEvent> iter = mEventQueue.iterator();
        MotionEvent prevEvent = iter.next();
        float totalVelocity = 0;
        for (int i = 0; i < MIN_STEPS; i++) {
          MotionEvent currEvent = iter.next();
          float time = elapsedTime(prevEvent, currEvent);
          float distance = MyMath.distanceBetween(rawLocation(prevEvent),
              rawLocation(currEvent));
          float distanceInInches = distance
              / MyActivity.getResolutionInfo().inchesToPixelsUI(1);
          totalVelocity += distanceInInches / time;
          prevEvent = currEvent;
        }
        float avgVelocity = totalVelocity / MIN_STEPS;
        boolean isGesture = (avgVelocity > 1.2f);
        if (mTracker != null) {
          mTracker.print(" avg velocity (inches/sec): " + d(avgVelocity));
          mTracker.setDecision(isGesture);
        }
        if (isGesture) {
          setState(STATE_RECORDING);
          processRecordingState(event);
        } else {
          setState(STATE_FORWARDING);
          processForwardingState(event);
        }
        return;
      }
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
    trace("GestureEventFilter, onTouch event " + UITools.dump(event)
        + ", passing events: " + d(mPassingEventFlag));
    if (event.getActionMasked() != MotionEvent.ACTION_MOVE)
      trace("onTouch: " + UITools.dump(event) + " state " + stateName(state()));

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
      // If panel is maximized, minimize it
      if (floatingViewMode()) {
        floatingPanel().setMinimized(true);
        return;
      }

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
    if (floatingViewMode()) {
      floatingPanel().setGesture(mMatch.strokeSet());
    }
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

  /**
   * For diagnosing gesture filter decision
   */
  private class DecisionTracker {
    public void clear() {
      mBuffer.setLength(0);
    }

    public void setDecision(boolean isGesture) {
      print(" is gesture: " + d(isGesture));

      System.out.println("\n----------------");
      System.out.println(mBuffer);
      System.out.println("\n");
    }

    public void addEvent(MotionEvent event) {
      MotionEvent startEvent = mEventQueue.peekFirst();
      if (startEvent == null)
        startEvent = event;
      Point loc = rawLocation(event);
      MotionEvent prevEvent = mEventQueue.peekLast();
      if (prevEvent == null) {
        clear();
        print("Initial event: " + UITools.dump(event));
        return;
      }
      float timeSincePrev = elapsedTime(prevEvent, event);
      Point prevLoc = rawLocation(prevEvent);
      float velocity = MyMath.distanceBetween(loc, prevLoc) / timeSincePrev;
      print(" t:" + d(timeSincePrev) + " v:" + d(velocity));
    }

    private void print(String message) {
      mBuffer.append(message);
      mBuffer.append('\n');
    }

    private StringBuilder mBuffer = new StringBuilder();
  }

  // Stroke set from user touch event
  private StrokeSet mTouchStrokeSet;
  private Listener mListener;
  private long mStartEventTimeMillis;

  private static Handler sHandler = new Handler();
  private boolean mTraceActive;
  private Deque<MotionEvent> mEventQueue = new ArrayDeque();
  private boolean mPassingEventFlag;
  private int mState;
  private GestureSet mStrokeSetCollection;
  private Match mMatch;
  private DecisionTracker mTracker;
  private boolean mFloatingViewMode;
  private GesturePanel mGesturePanel;
}
