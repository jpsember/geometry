package com.js.android;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.js.editor.UserEvent;
import com.js.editor.UserEventSource;
import com.js.geometry.IPoint;

import static com.js.basic.Tools.*;

/**
 * Listens to mouse events within a View, and generates corresponding UserEvents
 */
public class TouchEventGenerator implements OnTouchListener {

  /**
   * State machine representing our filter on Android touch events. It imposes a
   * small delay when a 'down' event occurs, to determine if it's part of a
   * multitouch event sequence.
   */
  private static final int //
      STATE_START = 0,//
      STATE_WAIT = 1,//
      STATE_MULTITOUCH = 2,//
      STATE_TOUCH = 3,//
      STATE_TOTAL = 4;

  private static final long MULTITOUCH_TIMEOUT_MS = 200;

  public TouchEventGenerator(UserEventSource eventSource) {
    mUserEventSource = eventSource;
  }

  private void sendEvent(int type) {
    sendEvent(type, mCurrentTouchLocation, 0);
  }

  private void sendEvent(int type, IPoint viewPoint) {
    sendEvent(type, viewPoint, 0);
  }

  private void sendEvent(int type, IPoint viewPoint, int modifierFlags) {
    UserEvent event = new UserEvent(type, mUserEventSource, viewPoint,
        modifierFlags);
    event.getManager().processUserEvent(event);
  }

  @Override
  public boolean onTouch(View view, MotionEvent e) {
    if (alwaysFalse())
      view.performClick();
    IPoint viewPoint = new IPoint(e.getX(), e.getY());

    mCurrentTouchLocation = viewPoint;
    int action = e.getActionMasked();

    switch (mTouchState) {
    case STATE_START: {
      switch (action) {
      case MotionEvent.ACTION_DOWN:
        setTouchState(STATE_WAIT);
        mInitialTouchLocation = mCurrentTouchLocation;
        // Start a timer to aid in determining if this is a
        // multitouch action.
        // Assign the timer a unique identifier, to allow us to
        // detect stale timeout events
        mTimeoutIdentifier++;
        final int expectedTimeoutIdentifier = mTimeoutIdentifier;
        mHandler.postDelayed(new Runnable() {
          public void run() {
            // Ignore if it's a stale event
            if (mTimeoutIdentifier != expectedTimeoutIdentifier)
              return;
            // If we're still in the WAIT state, switch to TOUCH
            if (mTouchState == STATE_WAIT) {
              setTouchState(STATE_TOUCH);
              sendEvent(UserEvent.CODE_DOWN, mInitialTouchLocation);
            }
          }
        }, MULTITOUCH_TIMEOUT_MS);
        break;
      }
    }
      break;

    case STATE_WAIT: {
      switch (action) {
      case MotionEvent.ACTION_MOVE:
        setTouchState(STATE_TOUCH);
        sendEvent(UserEvent.CODE_DOWN);
        sendEvent(UserEvent.CODE_DRAG);
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        setTouchState(STATE_MULTITOUCH);
        sendEvent(UserEvent.CODE_DOWN, mCurrentTouchLocation,
            UserEvent.FLAG_MULTITOUCH);
        break;
      case MotionEvent.ACTION_UP:
        setTouchState(STATE_TOUCH);
        sendEvent(UserEvent.CODE_DOWN);
        sendEvent(UserEvent.CODE_UP);
        setTouchState(STATE_START);
        break;
      }
      break;
    }

    case STATE_TOUCH: {
      switch (action) {
      case MotionEvent.ACTION_MOVE:
        sendEvent(UserEvent.CODE_DRAG);
        break;
      case MotionEvent.ACTION_UP:
        sendEvent(UserEvent.CODE_UP);
        setTouchState(STATE_START);
        break;
      }
    }
      break;

    case STATE_MULTITOUCH: {
      switch (action) {
      case MotionEvent.ACTION_MOVE:
        sendEvent(UserEvent.CODE_DRAG, mCurrentTouchLocation,
            UserEvent.FLAG_RIGHT);
        break;
      case MotionEvent.ACTION_UP:
        sendEvent(UserEvent.CODE_UP, mCurrentTouchLocation,
            UserEvent.FLAG_MULTITOUCH);
        setTouchState(STATE_START);
        break;
      }
    }
      break;
    }
    return true;
  }

  private void setTouchState(int s) {
    ASSERT(s >= 0 && s < STATE_TOTAL);
    mTouchState = s;
  }

  private int mTouchState = STATE_START;
  private Handler mHandler = new Handler();
  private int mTimeoutIdentifier;
  private IPoint mInitialTouchLocation;
  private IPoint mCurrentTouchLocation;

  private UserEventSource mUserEventSource;
}
