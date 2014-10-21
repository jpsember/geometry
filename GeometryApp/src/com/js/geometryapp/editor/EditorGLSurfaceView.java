package com.js.geometryapp.editor;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.view.MotionEvent;

import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.ConcreteStepper;
import static com.js.basic.Tools.*;

public class EditorGLSurfaceView extends GLSurfaceView {

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

	public EditorGLSurfaceView(Context context) {
		super(context);
		setEGLContextClientVersion(2);
	}

	@Override
	public void setRenderer(Renderer renderer) {
		mRenderer = (AlgorithmRenderer) renderer;
		super.setRenderer(renderer);
	}

	public void setEditor(Editor editor) {
		mEditor = editor;
	}

	@Override
	public boolean onTouchEvent(final MotionEvent e) {
		// Make sure we're synchronized with the OpenGL thread
		ConcreteStepper mStepper = mEditor.getStepper();
		// We need to have a bogus call to performClick() to prevent compile
		// warnings
		boolean mAlwaysFalse = db;
		if (mAlwaysFalse)
			performClick();
		boolean mTouchEventResult;
		synchronized (mStepper.getLock()) {
			mStepper.acquireLock();
			mTouchEventResult = onTouchEventAux(e);
			mStepper.releaseLock();
		}
		return mTouchEventResult;
	}

	private boolean onTouchEventAux(MotionEvent e) {
		final boolean db = TOUCH_DIAGNOSTICS;

		Point viewPoint = new Point(e.getX(), e.getY());

		// Transform point from device to algorithm coordinates

		Matrix deviceToAlgorithMatrix = mRenderer
				.getTransform(AlgorithmRenderer.TRANSFORM_NAME_DEVICE_TO_ALGORITHM);
		viewPoint.apply(deviceToAlgorithMatrix);

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
							sendEvent(new EditorEvent(EditorEvent.CODE_DOWN,
									mInitialTouchLocation));
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
				sendEvent(EditorEvent.CODE_DOWN);
				sendEvent(EditorEvent.CODE_DRAG);
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				setTouchState(STATE_MULTITOUCH);
				sendEvent(new EditorEvent(EditorEvent.CODE_DOWN,
						mCurrentTouchLocation, true));
				break;
			case MotionEvent.ACTION_UP:
				setTouchState(STATE_TOUCH);
				sendEvent(EditorEvent.CODE_DOWN);
				sendEvent(EditorEvent.CODE_UP);
				setTouchState(STATE_START);
				break;
			}
			break;
		}

		case STATE_TOUCH: {
			switch (action) {
			case MotionEvent.ACTION_MOVE:
				sendEvent(EditorEvent.CODE_DRAG);
				break;
			case MotionEvent.ACTION_UP:
				sendEvent(EditorEvent.CODE_UP);
				setTouchState(STATE_START);
				performClick();
				break;
			}
		}
			break;
		case STATE_MULTITOUCH: {
			switch (action) {
			case MotionEvent.ACTION_MOVE:
				sendEvent(new EditorEvent(EditorEvent.CODE_DRAG,
						mCurrentTouchLocation, true));
				break;
			case MotionEvent.ACTION_UP:
				sendEvent(new EditorEvent(EditorEvent.CODE_UP,
						mCurrentTouchLocation, true));
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
		sendEvent(new EditorEvent(code, mCurrentTouchLocation));
	}

	/**
	 * Send editor event to editor, with particular touch location
	 */
	private void sendEvent(EditorEvent event) {
		mEditor.processEvent(event);
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
	private AlgorithmRenderer mRenderer;
}
