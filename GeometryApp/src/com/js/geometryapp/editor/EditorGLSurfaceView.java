package com.js.geometryapp.editor;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.view.MotionEvent;

import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.opengl.OurGLSurfaceView;
import static com.js.basic.Tools.*;

public class EditorGLSurfaceView extends OurGLSurfaceView {

	private static final boolean TOUCH_DIAGNOSTICS = false && DEBUG_ONLY_FEATURES;

	/**
	 * State machine representing our filter on Android touch events. It imposes
	 * a small delay when a 'down' event occurs, to determine if it's part of a
	 * multitouch event sequence.
	 */
	private static final int //
			STATE_START = 0,//
			STATE_WAIT = 1,//
			STATE_MULTITOUCH = 2,//
			STATE_TOUCH = 3,//
			STATE_TOTAL = 4;

	private static final long MULTITOUCH_TIMEOUT_MS = 200;

	public EditorGLSurfaceView(Context context, GLSurfaceView.Renderer renderer) {
		super(context, renderer);
	}

	public void setEditor(Editor editor) {
		mEditor = editor;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		final boolean db = TOUCH_DIAGNOSTICS;

		Point viewPoint = new Point(e.getX(), e.getY());

		// Transform point from device to algorithm coordinates
		{
			AlgorithmRenderer renderer = (AlgorithmRenderer) renderer();
			Matrix deviceToAlgorithMatrix = renderer
					.getTransform(AlgorithmRenderer.TRANSFORM_NAME_DEVICE_TO_ALGORITHM);
			viewPoint.apply(deviceToAlgorithMatrix);
		}

		mCurrentTouchLocation = viewPoint;
		int action = e.getActionMasked();

		if (db) {
			if (action == MotionEvent.ACTION_DOWN)
				pr("\n\n\n\n");
			int ptrCount = e.getPointerCount();
			pr("MotionEvent (state " + d(mTouchState, 10) + ")" + d(action)
					+ " at " + mCurrentTouchLocation + ":  "
					+ motionEventName(action)
					+ (ptrCount > 1 ? "(ptrs=" + ptrCount + ")" : ""));
		}

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
							sendEvent(EditorEventListener.EVENT_DOWN,
									mInitialTouchLocation);
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
				sendEvent(EditorEventListener.EVENT_DOWN);
				sendEvent(EditorEventListener.EVENT_DRAG);
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				setTouchState(STATE_MULTITOUCH);
				sendEvent(EditorEventListener.EVENT_DOWN_MULTIPLE);
				break;
			case MotionEvent.ACTION_UP:
				setTouchState(STATE_TOUCH);
				sendEvent(EditorEventListener.EVENT_DOWN);
				sendEvent(EditorEventListener.EVENT_UP);
				setTouchState(STATE_START);
				break;
			}
			break;
		}

		case STATE_TOUCH: {
			switch (action) {
			case MotionEvent.ACTION_MOVE:
				sendEvent(EditorEventListener.EVENT_DRAG);
				break;
			case MotionEvent.ACTION_UP:
				sendEvent(EditorEventListener.EVENT_UP);
				setTouchState(STATE_START);
				performClick();
				break;
			}
		}
			break;
		case STATE_MULTITOUCH: {
			switch (action) {
			case MotionEvent.ACTION_MOVE:
				sendEvent(EditorEventListener.EVENT_DRAG_MULTIPLE);
				break;
			case MotionEvent.ACTION_UP:
				sendEvent(EditorEventListener.EVENT_UP_MULTIPLE);
				setTouchState(STATE_START);
				performClick();
				break;
			}
		}
			break;
		}
		return true;
	}

	/**
	 * Utility method to get name of a motion event action
	 */
	private String motionEventName(int action) {
		if (TOUCH_DIAGNOSTICS) {
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				return "DOWN";
			case MotionEvent.ACTION_MOVE:
				return "MOVE";
			case MotionEvent.ACTION_UP:
				return "UP";
			case MotionEvent.ACTION_POINTER_DOWN:
				return "DOWNx";
			case MotionEvent.ACTION_POINTER_UP:
				return "UPx";
			default:
				return "???";
			}
		}
		return null;
	}

	private void setTouchState(int s) {
		ASSERT(s >= 0 && s < STATE_TOTAL);
		mTouchState = s;
	}

	/**
	 * Send editor event to editor, with current touch location
	 */
	private void sendEvent(int code) {
		sendEvent(code, mCurrentTouchLocation);
	}

	/**
	 * Send editor event to editor, with particular touch location
	 */
	private void sendEvent(int code, Point location) {
		mEditor.processEvent(code, location);
	}

	@Override
	public boolean performClick() {
		// Calls the super implementation, which generates an
		// AccessibilityEvent and calls the onClick() listener on the view,
		// if any
		super.performClick();
		// Handle the action for the custom click here
		return true;
	}

	private int mTouchState = STATE_START;
	private Handler mHandler = new Handler();
	private int mTimeoutIdentifier;
	private Point mInitialTouchLocation;
	private Point mCurrentTouchLocation;
	private Editor mEditor;
}
