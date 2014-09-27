package com.js.geometryapp;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

import com.js.android.MyActivity;
import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.json.JSONTools;

public class AlgorithmStepper {

	// Simulate what performance will be if we replace update()+show() with just
	// show()
	static final boolean UPDATE_EXPERIMENT = false;

	public static interface Delegate {
		public void prepareOptions();

		public void runAlgorithm();

		public void displayResults();
	}

	/**
	 * Get the singleton instance of the stepper
	 */
	public static AlgorithmStepper sharedInstance() {
		if (sStepper == null) {
			if (UPDATE_EXPERIMENT)
				warning("UPDATE_EXPERIMENT is true");
			sStepper = new AlgorithmStepper();
		}
		return sStepper;
	}

	/**
	 * Set the delegate, which actually performs the algorithm, and displays it
	 */
	void setDelegate(Delegate delegate) {
		mDelegate = delegate;
		// Now that views have been built, restore option values
		prepareOptionsAux();

		resetStep();
	}

	/**
	 * Determine if algorithm stepper is active (i.e. hooked up to a control
	 * panel and controlling the progress of the calling algorithm)
	 */
	public boolean isActive() {
		return mActive;
	}

	/**
	 * Save current stepper active state on a stack, and push a new value (which
	 * is AND'd with previous state)
	 */
	public void pushActive(boolean active) {
		mActiveStack.add(mActive);
		mActive &= active;
	}

	public void popActive() {
		if (mActiveStack.isEmpty())
			throw new IllegalStateException("active stack is empty");
		mActive = pop(mActiveStack);
	}

	/**
	 * Push active state to value of checkbox widget
	 * 
	 * @param widgetId
	 *            id of checkbox widget to read
	 */
	public void pushActive(String widgetId) {
		boolean value = mActive;
		if (value)
			value = AlgorithmOptions.sharedInstance().getBooleanValue(widgetId);
		pushActive(value);
	}

	/**
	 * Determine if we should stop and display this frame of the current
	 * algorithm; should be followed by a call to show() if this returns true
	 */
	public boolean step() {
		return stepAux(false);
	}

	private boolean stepAux(boolean milestone) {
		boolean output = false;
		do {
			if (isActive()) {
				if (!mTotalStepsKnown) {
					if (milestone)
						addMilestone(mCurrentStep);
					mCurrentStep++;
				} else {
					if (mCurrentStep == mTargetStep) {
						output = true;
						break;
					}
					if (mTargetStep < mCurrentStep)
						die("target " + mTargetStep + " but current "
								+ mCurrentStep);
					mCurrentStep++;
				}
			}
		} while (false);

		if (UPDATE_EXPERIMENT) {
			mActualUpdateValue = output;
			return true;
		}
		return output;
	}

	/**
	 * Perform step(), but with a step that is a milestone
	 */
	public boolean bigStep() {
		return stepAux(true);
	}

	private boolean mActualUpdateValue;

	/**
	 * Generate an algorithm step. For efficiency, should only be called if
	 * step() returned true
	 * 
	 * @param message
	 *            message to display, which may cause other elements to be
	 *            displayed via side effects
	 */
	public void show(Object message) {
		if (UPDATE_EXPERIMENT) {
			if (!mActualUpdateValue) {
				clearRenderElements();
				return;
			}
		}
		if (mTotalStepsKnown) {
			String messageString = message.toString();
			mFrameTitle = messageString;
			throw new DesiredStepReachedException("reached desired step; "
					+ messageString);
		}
	}

	/**
	 * Add an element to be displayed with this algorithm frame
	 * 
	 * @param element
	 * @return an empty string, as a convenience so elements can be added as a
	 *         side effect of constructing show(...) message arguments
	 */
	public String plotElement(AlgorithmDisplayElement element) {
		do {
			if (AlgorithmDisplayElement.rendering()) {
				element.render();
				break;
			}

			// Do nothing if we're running just to calculate the total steps
			if (!mTotalStepsKnown)
				break;

			// If there's an active background plot key, store as background
			// element instead of adding to this frame
			if (mNextPlotKey != null) {
				mBackgroundElements.put(mNextPlotKey, element);
				clearPlotToBackground();
			} else {
				mDisplayElements.add(element);
			}
		} while (false);
		return "";
	}

	/**
	 * Add the next plotted element to the background
	 * 
	 * @param key
	 *            key to identify this background element from others
	 */
	public void plotToBackground(String key) {
		// Do nothing if we're running just to calculate the total steps
		if (!mTotalStepsKnown)
			return;
		AlgorithmDisplayElement.resetRenderStateVars();
		mNextPlotKey = key;
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

	public String plot(Point point, float radius) {
		return plotElement(new PointElement(point, radius));
	}

	public String plot(Polygon polygon) {
		return plot(polygon, false);
	}

	public String plot(Polygon polygon, boolean filled) {
		return plotElement(new PolygonElement(polygon,
				filled ? PolygonElement.Style.FILLED
						: PolygonElement.Style.BOUNDARY));
	}

	public String plotPolyline(Collection<Point> endpoints, boolean closed) {
		return plotElement(new PolygonElement(new Polygon(endpoints),
				closed ? PolygonElement.Style.BOUNDARY
						: PolygonElement.Style.POLYLINE));
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

	public void requestUpdate() {
		requestUpdate(false);
	}

	public void requestUpdate(boolean recalculateTotalSteps) {
		if (recalculateTotalSteps) {
			// Run algorithm to completion to determine the number of steps
			mTotalStepsKnown = false;
			performAlgorithm();
			setTotalStepsKnown();

			// Propagate these values to the stepper control panel (without
			// causing a recursive update)
			updateStepperView(false);
		}
		synchronized (AlgorithmStepper.getLock()) {
			performAlgorithm();
			mDelegate.displayResults();
		}
	}

	public Rect algorithmRect() {
		if (mAlgorithmRect == null) {
			DisplayMetrics m = MyActivity.displayMetrics();
			if (m.widthPixels > m.heightPixels) {
				setAlgorithmRect(new Rect(0, 0, 1200, 1000));
			} else {
				setAlgorithmRect(new Rect(0, 0, 1000, 1200));
			}
		}
		return mAlgorithmRect;
	}

	public void setAlgorithmRect(Rect r) {
		mAlgorithmRect = r;
	}

	private void resetStep() {
		// Run algorithm to completion to determine the number of steps
		mTotalStepsKnown = false;
		performAlgorithm();
		setTotalStepsKnown();
		updateStepperView(true);
	}

	private void setTotalStepsKnown() {
		mTotalStepsKnown = true;
		mTotalSteps = mCurrentStep;
		// Clamp previous target step into new range
		mTargetStep = MyMath.clamp(mTargetStep, 0, mTotalSteps - 1);
	}

	/**
	 * Print a warning if no delegate has been defined
	 */
	void verifyDelegateDefined() {
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
	View controllerView(Context context) {
		if (mStepperView == null) {
			mStepperView = new AlgorithmStepperView(context, this);
		}
		return mStepperView.view();
	}

	/**
	 * Dispose of the controller's view; should be called when activity's
	 * onDestroy() is called
	 */
	void destroy() {
		mTotalStepsKnown = false;
		mStepperView = null;
	}

	/**
	 * Render algorithm frame, by plotting (and disposing of) any added
	 * elements, as well as the frame's title
	 */
	void render() {
		renderBackgroundElements();
		for (AlgorithmDisplayElement element : mDisplayElements) {
			element.render();
		}
		if (mFrameTitle != null) {
			AlgorithmDisplayElement.renderFrameTitle(mFrameTitle);
		}
		clearRenderElements();
		if (UPDATE_EXPERIMENT)
			pr("elements constructed: "
					+ AlgorithmDisplayElement.getElementCount());
	}

	private void clearRenderElements() {
		mDisplayElements.clear();
		mFrameTitle = null;
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
	 * Cancel the flag that causes the next element to be plotted to the
	 * background
	 */
	void clearPlotToBackground() {
		mNextPlotKey = null;
	}

	/**
	 * An exception of this type is thrown by the algorithm stepper when the
	 * target step is reached
	 */
	static class DesiredStepReachedException extends RuntimeException {
		public DesiredStepReachedException(String message) {
			super(message);
		}
	}

	private void performAlgorithm() {
		synchronized (AlgorithmStepper.getLock()) {
			try {
				initializeActiveState(true);

				mCurrentStep = 0;
				mNextPlotKey = null;
				mBackgroundElements.clear();
				mDisplayElements.clear();
				AlgorithmDisplayElement.resetRenderStateVars();

				if (!mTotalStepsKnown) {
					mMilestones.clear();
					addMilestone(mCurrentStep);
				}

				try {
					mDelegate.runAlgorithm();
				} catch (DesiredStepReachedException e) {
					throw e;
				} catch (Throwable t) {
					// Pop active stack until it's empty; we want to make sure
					// this message gets displayed, even if it occurred during a
					// sequence for which stepping is disabled
					while (!mActiveStack.isEmpty())
						popActive();
					bigStep();
					// Show message describing exception even if bigStep()
					// returned false for some reason
					pr(t + "\n" + stackTrace(t));
					show("Caught: " + t);
				}

				if (!mTotalStepsKnown) {
					addMilestone(mCurrentStep);
				}
			} catch (DesiredStepReachedException e) {
			} finally {
				initializeActiveState(false);
			}
		}
	}

	void setStepperView(AlgorithmStepperView view) {
		mStepperView = view;
		updateStepperView(true);
	}

	private AlgorithmStepper() {
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

	void setTargetStep(int step) {
		if (mIgnoreStepperView) {
			return;
		}
		mTargetStep = MyMath.clamp(step, 0, mTotalSteps - 1);
		updateStepperView(true);
	}

	void adjustTargetStep(int delta) {
		int seekStep = mTargetStep + delta;
		setTargetStep(seekStep);
	}

	void adjustDisplayedMilestone(int delta) {
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

	int targetStep() {
		return mTargetStep;
	}

	int totalSteps() {
		return mTotalSteps;
	}

	void prepareOptions() {
		if (mDelegate == null)
			die("attempt to prepare options before delegate defined");
		mDelegate.prepareOptions();
	}

	private void initializeActiveState(boolean active) {
		mActive = active;
		mActiveStack.clear();
	}

	private void prepareOptionsAux() {
		AlgorithmOptions mOptions = AlgorithmOptions.sharedInstance();
		// Add a hidden widget to persist the target step
		mOptions.addWidgets(JSONTools
				.swapQuotes("[{'id':'targetstep','type':'slider','hidden':true}]"));

		prepareOptions();

		mOptions.registerAlgorithmDetailListeners();
		mOptions.restoreStepperState();

		setTargetStep(mOptions.getIntValue("targetstep"));
	}

	/**
	 * Get the singleton object that serves as the synchronization lock to avoid
	 * race conditions between the UI and OpenGL threads
	 * 
	 * @return
	 */
	static Object getLock() {
		return sSynchronizationLock;
	}

	// The singleton instance of this class
	private static AlgorithmStepper sStepper;
	private static Object sSynchronizationLock = new Object();

	private ArrayList<AlgorithmDisplayElement> mDisplayElements = new ArrayList();
	private Map<String, AlgorithmDisplayElement> mBackgroundElements = new HashMap();
	private String mFrameTitle;
	private boolean mIgnoreStepperView;
	private ArrayList<Integer> mMilestones = new ArrayList();
	private int mTargetStep;
	private int mCurrentStep;
	private int mTotalSteps;
	// If false, all calls to step() return false
	private boolean mActive;
	private ArrayList<Boolean> mActiveStack = new ArrayList();
	private boolean mTotalStepsKnown;
	private AlgorithmStepperView mStepperView;
	private Delegate mDelegate;
	private String mNextPlotKey;
	private Rect mAlgorithmRect;
}
