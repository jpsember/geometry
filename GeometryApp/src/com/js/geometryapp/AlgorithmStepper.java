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
import com.js.geometry.Mesh;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.SliderWidget;

public class AlgorithmStepper {

	private static final boolean MILESTONES_ENABLED = false;

	/**
	 * Enables diagnostic printing related to target, total steps
	 */
	private static final boolean DIAGNOSE_STEPS = true;

	static final String WIDGET_ID_TOTALSTEPS = "_steps_";
	static final String WIDGET_ID_TARGETSTEP = "_target_";
	static final String WIDGET_ID_JUMP_BWD = "<<";
	static final String WIDGET_ID_JUMP_FWD = ">>";
	static final String WIDGET_ID_STEP_BWD = "<";
	static final String WIDGET_ID_STEP_FWD = ">";

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
			sStepper = new AlgorithmStepper();
		}
		return sStepper;
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
			value = sOptions.getBooleanValue(widgetId);
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
				if (MILESTONES_ENABLED) {
					if (milestone)
						addMilestone(mCurrentStep);
				}
				if (mCurrentStep == mTargetStep) {
					output = true;
					break;
				}
				if (mTargetStep < mCurrentStep)
					die("target " + mTargetStep + " but current "
							+ mCurrentStep);
				mCurrentStep++;
			}
		} while (false);

		return output;
	}

	/**
	 * Perform step(), but with a step that is a milestone
	 */
	public boolean bigStep() {
		return stepAux(true);
	}

	/**
	 * Generate an algorithm step. For efficiency, should only be called if
	 * step() returned true
	 * 
	 * @param message
	 *            message to display, which may cause other elements to be
	 *            displayed via side effects
	 */
	public void show(Object message) {
		String messageString = message.toString();
		if (DIAGNOSE_STEPS) {
			messageString = "#" + f(mCurrentStep, 4) + "/" + f(mTotalSteps, 4)
					+ " " + messageString;
		}
		mFrameTitle = messageString;
		throw new DesiredStepReachedException(messageString);
	}

	/**
	 * Open a background layer. Subsequent plot() commands will be redirected to
	 * this layer. Must be balanced by a call to closeLayer(). Layers are
	 * plotted in alphabetical order, so the last layer plotted is topmost in
	 * the view. Once defined, layers will appear in every rendered frame, in
	 * addition to step-specific elements
	 * 
	 * @param key
	 *            uniquely distinguishes this layer from others
	 */
	public void openLayer(String key) {
		if (mActiveBackgroundLayer != null)
			throw new IllegalStateException("layer already open");
		AlgorithmDisplayElement.resetRenderStateVars();
		mActiveBackgroundLayer = new Layer(key);
		mBackgroundLayers.put(key, mActiveBackgroundLayer);
	}

	/**
	 * Close layer previously opened via openLayer()
	 */
	public void closeLayer() {
		AlgorithmDisplayElement.resetRenderStateVars();
		mActiveBackgroundLayer = null;
	}

	/**
	 * Remove a layer, so it will no longer be plotted
	 * 
	 * @param key
	 */
	public void removeLayer(String key) {
		mBackgroundLayers.remove(key);
	}

	/**
	 * Add an element to be displayed with this algorithm frame
	 * 
	 * @param element
	 * @return an empty string, as a convenience so elements can be added as a
	 *         side effect of constructing show(...) message arguments
	 */
	public String plot(AlgorithmDisplayElement element) {
		do {
			if (AlgorithmDisplayElement.rendering()) {
				element.render();
				break;
			}

			// If there's an active background layer, add it to that instead
			Layer targetLayer = mActiveBackgroundLayer;
			if (targetLayer == null)
				targetLayer = mForegroundLayer;
			targetLayer.add(element);

		} while (false);
		return "";
	}

	public String plotRay(Point p1, Point p2) {
		return plot(new RayElement(p1, p2));
	}

	public String plotLine(Point p1, Point p2) {
		return plot(new LineElement(p1, p2));
	}

	public String plot(Point point) {
		return plot(point, 1);
	}

	public String plot(Point point, float radius) {
		return plot(new PointElement(point, radius));
	}

	public String plot(Polygon polygon) {
		return plot(polygon, false);
	}

	public String plot(Polygon polygon, boolean filled) {
		return plot(new PolygonElement(polygon,
				filled ? PolygonElement.Style.FILLED
						: PolygonElement.Style.BOUNDARY));
	}

	public String plotPolyline(Collection<Point> endpoints) {
		return plot(new PolygonElement(new Polygon(endpoints),
				PolygonElement.Style.POLYLINE));
	}

	public String plotMesh(Mesh meshContext) {
		return plot(new MeshElement(meshContext));
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
	 * Request a refresh of the algorithm display. Runs to the target step (if
	 * possible) and displays that frame.
	 */
	public void refresh() {
		synchronized (AlgorithmStepper.getLock()) {
			performAlgorithm();
			mDelegate.displayResults();
		}
	}

	private void setTargetStepField(int value) {
		ASSERT(value >= 0);
		if (mTargetStep == value)
			return;
		mTargetStep = value;
		// Update widget that is persisting this value
		sOptions.setValue(WIDGET_ID_TARGETSTEP, mTargetStep);
	}

	/**
	 * Set the delegate, which actually performs the algorithm, and displays it
	 */
	void setDelegate(Delegate delegate) {
		mDelegate = delegate;
		// Now that views have been built, restore option values
		prepareOptionsAux();
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
	 * Construct stepper controller view
	 */
	View controllerView(Context context) {
		return AlgorithmStepperPanel.build(context);
	}

	/**
	 * Render algorithm frame, by plotting (and disposing of) any added
	 * elements, as well as the frame's title
	 */
	void render() {
		renderBackgroundElements();
		mForegroundLayer.render();
		if (mFrameTitle != null) {
			AlgorithmDisplayElement.renderFrameTitle(mFrameTitle);
		}
	}

	/**
	 * Render persistent (background) elements, in order of sorted keys
	 */
	private void renderBackgroundElements() {
		ArrayList<String> keys = new ArrayList(mBackgroundLayers.keySet());
		Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
		for (String key : keys) {
			Layer layer = mBackgroundLayers.get(key);
			layer.render();
		}
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

	private String dumpStepInfo() {
		return "[ Current:" + f(mCurrentStep, 4, true) + " Target:"
				+ f(mTargetStep, 4, true) + " Total:" + f(mTotalSteps, 4, true)
				+ " ] ";
	}

	private void performAlgorithm() {
		final boolean db = DIAGNOSE_STEPS;

		synchronized (AlgorithmStepper.getLock()) {
			try {
				initializeActiveState(true);

				mCurrentStep = 0;
				mActiveBackgroundLayer = null;
				mBackgroundLayers.clear();
				mForegroundLayer.clear();
				mFrameTitle = null;

				AlgorithmDisplayElement.resetRenderStateVars();

				if (MILESTONES_ENABLED) {
					mMilestones.clear();
					addMilestone(mCurrentStep);
				} else {
					warning("milestones have been disabled");
				}

				boolean completed = false;
				try {
					mDelegate.runAlgorithm();

					// We completed the algorithm without halting.

					// We're about to throw an exception that will be caught
					// below; set flag so that we know we completed without
					// halting.
					completed = true;

					// If the target step was not the maximum, the maximum is
					// too high.
					if (mCurrentStep < mTotalSteps) {
						if (db)
							pr("Completed algorithm; "
									+ dumpStepInfo()
									+ "\n  total steps was too high, setting to current");
						mTotalSteps = mCurrentStep;
						mTargetStep = mTotalSteps;
					}
					// Always end an algorithm with a bigStep/show combination
					if (bigStep()) { // should always return true
						show("Done");
					} else {
						die("unexpected!");
					}
				} catch (DesiredStepReachedException e) {
					if (!completed) {
						// We halted without completing. If we halted on what we
						// thought was the last step, the total steps is too
						// low.
						if (mCurrentStep == mTotalSteps) {
							pr("Halted, total steps was too low, increasing; "
									+ dumpStepInfo());
							mTotalSteps = (int) (Math.max(mTotalSteps, 50) * 1.3f);
						}
					}
					throw e;
				} catch (Throwable t) {
					// Pop active stack until it's empty; we want to make sure
					// this message gets displayed, even if it occurred during a
					// sequence for which stepping is disabled
					while (!mActiveStack.isEmpty())
						popActive();

					pr(t + "\n" + stackTrace(t));

					if (bigStep()) {
						show("Caught: " + t);
					}
				}
			} catch (DesiredStepReachedException e) {
			} finally {
				initializeActiveState(false);
			}

			updateAlgorithmLength();
		}
	}

	private AlgorithmStepper() {
		unimp("if desired step is presumed last step, don't halt since we want to run to completion");
	}

	/**
	 * Update target step slider maximum and current values to be consistent
	 * with our estimates
	 */
	private void updateAlgorithmLength() {
		final boolean db = DIAGNOSE_STEPS;

		SliderWidget wTotal = sOptions.getWidget(WIDGET_ID_TOTALSTEPS);
		int currentTotalSteps = wTotal.getIntValue();
		SliderWidget wTarget = sOptions.getWidget(WIDGET_ID_TARGETSTEP);
		int currentTargetStep = wTarget.getIntValue();

		if (currentTotalSteps != mTotalSteps
				|| currentTargetStep != mTargetStep) {
			if (db) {
				pr("updateAlgorithmLength, previous target "
						+ currentTargetStep + " of total " + currentTotalSteps
						+ "; updating to " + dumpStepInfo());
			}
			// While changing the total steps, the controller view may try
			// to change the target step on us; ignore such events
			mIgnoreStepperView = true;
			wTarget.setMaxValue(mTotalSteps);
			wTotal.setValue(mTotalSteps);
			wTarget.setValue(mTargetStep);
			mIgnoreStepperView = false;
		}
	}

	private void setTargetStep(int step) {
		ASSERT(mTotalSteps > 0);
		if (mIgnoreStepperView) {
			return;
		}
		setTargetStepField(MyMath.clamp(step, 0, mTotalSteps));
		updateAlgorithmLength();
	}

	private void adjustTargetStep(int delta) {
		int seekStep = mTargetStep + delta;
		setTargetStep(seekStep);
	}

	private void adjustDisplayedMilestone(int delta) {
		if (!MILESTONES_ENABLED)
			return;
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

	private void initializeActiveState(boolean active) {
		mActive = active;
		mActiveStack.clear();
	}

	private void addStepperViewListeners() {
		// Issue #68:
		// It would be nice if we could do this when the stepper view is
		// constructed; this would entail building the options view sooner.
		sOptions.getWidget(WIDGET_ID_TARGETSTEP).addListener(
				new AbstractWidget.Listener() {
					@Override
					public void valueChanged(AbstractWidget widget) {
						setTargetStep(widget.getIntValue());
					}
				});

		final String[] ids = { WIDGET_ID_JUMP_BWD, WIDGET_ID_JUMP_FWD,
				WIDGET_ID_STEP_BWD, WIDGET_ID_STEP_FWD };

		AbstractWidget.Listener listener = new AbstractWidget.Listener() {
			@Override
			public void valueChanged(AbstractWidget widget) {
				String id = widget.getId();
				for (int j = 0; j < 2; j++) {
					if (id == ids[j])
						adjustDisplayedMilestone(j == 0 ? -1 : 1);
					if (id == ids[j + 2])
						adjustTargetStep(j == 0 ? -1 : 1);
				}
			}
		};
		for (int i = 0; i < ids.length; i++) {
			sOptions.getWidget(ids[i]).addListener(listener);
		}
	}

	private void prepareOptionsAux() {
		sOptions = AlgorithmOptions.sharedInstance();

		// Add a detached widget to persist the total steps
		sOptions.addSlider(WIDGET_ID_TOTALSTEPS,
				AbstractWidget.OPTION_DETACHED, true,
				AbstractWidget.OPTION_RECALC_ALGORITHM_STEPS, false);

		addStepperViewListeners();

		if (mDelegate == null)
			die("attempt to prepare options before delegate defined");
		mDelegate.prepareOptions();

		sOptions.restoreStepperState();

		// Restore algorithm information from saved widget values, if possible
		mTotalSteps = sOptions.getIntValue(WIDGET_ID_TOTALSTEPS);
		mTargetStep = sOptions.getIntValue(WIDGET_ID_TARGETSTEP);
		if (mTotalSteps == 0) {
			mTotalSteps = 100;
			mTargetStep = 100;
		}
		mTargetStep = Math.min(mTargetStep, mTotalSteps);

		refresh();
	}

	/**
	 * Get the singleton object that serves as the synchronization lock to avoid
	 * race conditions between the UI and OpenGL threads
	 * 
	 * @return
	 */
	public static Object getLock() {
		return sSynchronizationLock;
	}

	private static class Layer {
		public Layer(String name) {
		}

		public void clear() {
			mElements.clear();
		}

		public ArrayList<AlgorithmDisplayElement> elements() {
			return mElements;
		}

		public void add(AlgorithmDisplayElement element) {
			mElements.add(element);
		}

		public void render() {
			for (AlgorithmDisplayElement element : elements())
				element.render();
		}

		private ArrayList<AlgorithmDisplayElement> mElements = new ArrayList();
	}

	// The singleton instance of this class
	private static AlgorithmStepper sStepper;
	private static Object sSynchronizationLock = new Object();
	private static AlgorithmOptions sOptions;

	private Layer mForegroundLayer = new Layer("_");
	private Map<String, Layer> mBackgroundLayers = new HashMap();
	private String mFrameTitle;
	private boolean mIgnoreStepperView;
	private ArrayList<Integer> mMilestones = new ArrayList();
	private int mTargetStep;
	private int mCurrentStep;
	private int mTotalSteps;
	private boolean mActive;
	private ArrayList<Boolean> mActiveStack = new ArrayList();
	private Delegate mDelegate;
	private Layer mActiveBackgroundLayer;
	private Rect mAlgorithmRect;
}
