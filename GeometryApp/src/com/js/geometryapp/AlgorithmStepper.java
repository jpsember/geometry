package com.js.geometryapp;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.view.View;

import com.js.android.AppPreferences;
import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Vertex;

public class AlgorithmStepper {

	public static interface Delegate {
		public void runAlgorithm();

		public void displayResults();
	}

	private static final String PERSIST_KEY_TARGET_STEP = "alg_step";

	/**
	 * Get the singleton instance of the stepper
	 */
	public static AlgorithmStepper sharedInstance() {
		if (sStepper == null) {
			sStepper = new AlgorithmStepper();
		}
		return sStepper;
	}

	public void restoreState() {
		mTargetStep = AppPreferences.getInt(PERSIST_KEY_TARGET_STEP, 0);
	}

	public void saveState() {
		// TODO: have hidden widget for holding this value?
		AppPreferences.putInt(PERSIST_KEY_TARGET_STEP, mTargetStep);
	}

	/**
	 * Set the delegate, which actually performs the algorithm, and displays it
	 */
	public void setDelegate(Delegate delegate) {
		mDelegate = delegate;
		resetStep();
	}

	public void resetStep() {
		// Run algorithm to completion to determine the number of steps
		mTotalStepsKnown = false;
		performAlgorithm();
		mTotalStepsKnown = true;
		mTotalSteps = mCurrentStep;
		updateStepperView(true);
	}

	/**
	 * Print a warning if no delegate has been defined
	 */
	protected void verifyDelegateDefined() {
		if (mDelegate == null) {
			warning("no algorithm delegate defined");
		}
	}

	/**
	 * Get the stepper controller view, constructing it if necessary
	 * 
	 * @param context
	 *            context to use, in case construction necessary
	 * @return view
	 */
	public View controllerView(Context context) {
		if (mStepperView == null) {
			mStepperView = new AlgorithmStepperView(context, this);
		}
		return mStepperView.view();
	}

	/**
	 * Determine if we should stop and display this frame of the current
	 * algorithm; should be followed by a call to update() if this returns true
	 */
	public boolean update() {
		if (isActive()) {
			if (!mTotalStepsKnown) {
				// We need to see what the message is, to determine if it's a
				// milestone. We won't throw an exception to end the algorithm
				// in this case.
				mCurrentStep++;
				return true;
			} else if (mCurrentStep == mTargetStep) {
				clearDisplayList();
				return true;
			}
			mCurrentStep++;
		}
		return false;
	}

	/**
	 * Generate an algorithm step. For efficiency, should only be called if
	 * update() returns true
	 * 
	 * @param message
	 *            message to display, which may cause other elements to be
	 *            displayed via side effects (not yet implemented)
	 */
	public void show(Object message) {
		String messageString = message.toString();
		String displayedMessageString = trimMilestonePrefix(messageString);
		if (!mTotalStepsKnown) {
			// We're only examining the message to see if it's a milestone
			if (displayedMessageString != messageString) {
				addMilestone(mCurrentStep - 1);
			}
		} else {
			mFrameTitle = displayedMessageString;
			throw new DesiredStepReachedException("reached desired step; "
					+ displayedMessageString);
		}
	}

	/**
	 * Dispose of the controller's view; should be called when activity's
	 * onDestroy() is called
	 */
	public void destroy() {
		mTotalStepsKnown = false;
		mStepperView = null;
	}

	/**
	 * Add an element to be displayed with this algorithm frame
	 * 
	 * @param element
	 * @return an empty string, as a convenience so elements can be added as a
	 *         side effect of constructing show(...) message arguments
	 */
	public String plotElement(AlgorithmDisplayElement element) {
		// If there's an active background plot key, store as background element
		// instead of adding to this frame
		if (mNextPlotKey != null) {
			mBackgroundElements.put(mNextPlotKey, element);
			clearPlotToBackground();
		} else {
			mDisplayElements.add(element);
		}
		return "";
	}

	/**
	 * Render algorithm frame, by plotting (and disposing of) any added
	 * elements, as well as the frame's title
	 */
	public void render() {
		renderBackgroundElements();
		for (AlgorithmDisplayElement element : mDisplayElements) {
			element.render();
		}
		mDisplayElements.clear();
		if (mFrameTitle != null) {
			AlgorithmDisplayElement.renderFrameTitle(mFrameTitle);
			mFrameTitle = null;
		}
	}

	/**
	 * Render persistent (background) elements, in order of sorted keys
	 */
	private void renderBackgroundElements() {
		ArrayList<String> keys = new ArrayList(mBackgroundElements.keySet());
		Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
		for (String key : keys) {
			AlgorithmDisplayElement element = mBackgroundElements.get(key);
			element.render();
		}
	}

	/**
	 * Add the next plotted element to the background
	 * 
	 * @param key
	 *            key to identify this background element from others
	 */
	public void plotToBackground(String key) {
		mNextPlotKey = key;
	}

	/**
	 * Cancel the flag that causes the next element to be plotted to the
	 * background
	 */
	public void clearPlotToBackground() {
		mNextPlotKey = null;
	}

	/**
	 * Remove a background element
	 * 
	 * @param key
	 *            element key
	 */
	public void removeBackgroundElement(String key) {
		mBackgroundElements.remove(key);
	}

	public String plotRay(Point p1, Point p2) {
		return plotElement(new RayElement(p1, p2));
	}

	public String plotLine(Point p1, Point p2) {
		return plotElement(new LineElement(p1, p2));
	}

	public String plot(Point point) {
		return plot(point, 1);
	}

	public String plot(Vertex vertex) {
		return plot(vertex.point());
	}

	public String plot(Point point, float radius) {
		return plotElement(new PointElement(point, radius));
	}

	public String plot(Polygon polygon) {
		return plot(polygon, false);
	}

	public String plot(Polygon polygon, boolean filled) {
		return plotElement(new PolygonElement(polygon, filled));
	}

	public String plot(GeometryContext meshContext) {
		return plotElement(new MeshElement(meshContext));
	}

	public String setColor(int color) {
		AlgorithmDisplayElement.setColorState(color);
		return "";
	}

	public String setLineWidth(float lineWidth) {
		AlgorithmDisplayElement.setLineWidthState(lineWidth);
		return "";
	}

	/**
	 * An exception of this type is thrown by the algorithm stepper when the
	 * target step is reached
	 */
	protected static class DesiredStepReachedException extends RuntimeException {
		public DesiredStepReachedException(String message) {
			super(message);
		}
	}

	/**
	 * Synchronized in case renderer is running in different thread
	 */
	private synchronized void performAlgorithm() {
		try {
			setActive(true);

			mCurrentStep = 0;
			mNextPlotKey = null;
			mBackgroundElements.clear();

			if (!mTotalStepsKnown) {
				mMilestones.clear();
				addMilestone(mCurrentStep);
			}

			mDelegate.runAlgorithm();
			if (!mTotalStepsKnown) {
				addMilestone(mCurrentStep);
			}
		} catch (DesiredStepReachedException e) {
		} finally {
			setActive(false);
		}
	}

	protected void setStepperView(AlgorithmStepperView view) {
		mStepperView = view;
		updateStepperView(true);
	}

	private AlgorithmStepper() {
	}

	private String trimMilestonePrefix(String message) {
		if (!message.startsWith("*"))
			return message;
		return message.substring(1);
	}

	/**
	 * Propagate values from this (the controller) to the controller's view.
	 */
	private void updateStepperView(boolean requestUpdateIfChanged) {
		if (mStepperView != null) {
			if (mTotalStepsKnown) {
				// While changing the total steps, the controller view may try
				// to change the target step on us; ignore such events
				mIgnoreStepperView = true;
				mStepperView.setTotalSteps(mTotalSteps);
				mIgnoreStepperView = false;
				mStepperView.setTargetStep(mTargetStep);
				if (requestUpdateIfChanged && mTargetStep != mCurrentStep) {
					requestUpdate();
				}
			}
		}
	}

	public void requestUpdate() {
		requestUpdate(false);
	}

	public void requestUpdate(boolean recalculateTotalSteps) {
		if (recalculateTotalSteps) {
			int previousTargetStep = mTargetStep;
			// Run algorithm to completion to determine the number of steps
			mTotalStepsKnown = false;
			performAlgorithm();
			mTotalStepsKnown = true;
			mTotalSteps = mCurrentStep;

			// Clamp previous target step into new range
			mTargetStep = MyMath.clamp(previousTargetStep, 0, mTotalSteps - 1);

			// Propagate these values to the stepper control panel (without
			// causing a recursive update)
			updateStepperView(false);
		}
		synchronized (this) {
			performAlgorithm();
			mDelegate.displayResults();
		}
	}

	protected void setTargetStep(int step) {
		if (mIgnoreStepperView) {
			return;
		}
		mTargetStep = MyMath.clamp(step, 0, mTotalSteps - 1);
		updateStepperView(true);
	}

	protected void adjustTargetStep(int delta) {
		int seekStep = mTargetStep + delta;
		setTargetStep(seekStep);
	}

	protected void adjustDisplayedMilestone(int delta) {
		int prev = -1, next = -1;
		for (int i = 0; i < mMilestones.size(); i++) {
			int k = mMilestones.get(i);
			if (k < mTargetStep)
				prev = k;
			if (k > mTargetStep && next < 0)
				next = k;
		}
		int seekStep = (delta < 0) ? prev : next;
		if (seekStep >= 0) {
			setTargetStep(seekStep);
		}
	}

	private void addMilestone(int n) {
		int lastMilestone = -1;
		if (!mMilestones.isEmpty()) {
			lastMilestone = mMilestones.get(mMilestones.size() - 1);
		}
		if (n != lastMilestone)
			mMilestones.add(n);
	}

	protected int targetStep() {
		return mTargetStep;
	}

	protected int totalSteps() {
		return mTotalSteps;
	}

	private void clearDisplayList() {
		mDisplayElements.clear();
	}

	private void setActive(boolean active) {
		mActive = active;
	}

	public boolean isActive() {
		return mActive;
	}

	// The singleton instance of this class
	private static AlgorithmStepper sStepper;

	private ArrayList<AlgorithmDisplayElement> mDisplayElements = new ArrayList();
	private Map<String, AlgorithmDisplayElement> mBackgroundElements = new HashMap();
	private String mFrameTitle;
	private boolean mIgnoreStepperView;
	private ArrayList<Integer> mMilestones = new ArrayList();
	private int mTargetStep;
	private int mCurrentStep;
	private int mTotalSteps;
	// If false, all calls to update() return false
	private boolean mActive;
	private boolean mTotalStepsKnown;
	private AlgorithmStepperView mStepperView;
	private Delegate mDelegate;
	private String mNextPlotKey;
}
