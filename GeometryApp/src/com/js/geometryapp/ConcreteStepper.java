package com.js.geometryapp;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.widget.LinearLayout;

import com.js.android.UITools;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.geometry.Renderable;
import com.js.geometry.Segment;
import com.js.geometryapp.editor.Editor;
import com.js.geometryapp.widget.AbstractWidget;

public class ConcreteStepper extends AlgorithmStepper {

	/**
	 * This debug-only flag, if true, performs additional tests to verify that
	 * the UI and OpenGL threads are cooperating.
	 */
	private static final boolean VERIFY_LOCK = (false && DEBUG_ONLY_FEATURES);

	static final String WIDGET_ID_JUMP_BWD = "<<";
	static final String WIDGET_ID_JUMP_FWD = ">>";
	static final String WIDGET_ID_STEP_BWD = "<";
	static final String WIDGET_ID_STEP_FWD = ">";

	private static final float HIGHLIGHT_LINE_WIDTH = 3.0f;

	void setDependencies(AlgorithmOptions options, Editor editor) {
		mOptions = options;
		mEditor = editor;
	}

	void setGLSurfaceView(GLSurfaceView glSurfaceView) {
		mglSurfaceView = glSurfaceView;
	}

	@Override
	public void addAlgorithm(Algorithm delegate) {
		mAlgorithms.add(delegate);
	}

	/**
	 * Set algorithm rect to best fit the space available
	 * 
	 * @param availableRect
	 *            rectangle available, in device space (aspect ratio is
	 *            significant, not the actual size)
	 */
	public void prepareAlgorithmRect(Rect availableRect) {
		float ar = availableRect.height / availableRect.width;
		if (ar > 1) {
			mAlgorithmRect = new Rect(0, 0, 1000, ar * 1000);
		} else {
			mAlgorithmRect = new Rect(0, 0, 1000 / ar, 1000);
		}
	}

	/**
	 * Set visible rectangle, in algorithm space; this is probably bigger than
	 * the algorithm rect, which was constructed to fit within it
	 * 
	 * @param visibleRect
	 */
	public void setVisibleRect(Rect visibleRect) {
		mVisibleRect = visibleRect;
	}

	@Override
	public Rect algorithmRect() {
		// If we haven't prepared a rectangle yet, the view hasn't been
		// displayed; just return the default (we'll request a refresh
		// when the rectangle IS prepared)
		if (!isAlgorithmRectPrepared())
			return super.algorithmRect();
		return mAlgorithmRect;
	}

	public Rect visibleRect() {
		ASSERT(mVisibleRect != null);
		return mVisibleRect;
	}

	@Override
	public void popActive() {
		if (isActive()) {
			removeCurrentBackgroundLayers();
		}
		super.popActive();
	}

	@Override
	public void pushActive(String widgetId) {
		boolean value = isActive();
		if (value)
			value = mOptions.getBooleanValue(widgetId);
		pushActive(value);
	}

	@Override
	public boolean step() {
		return stepAux(false);
	}

	private boolean stepAux(boolean milestone) {
		if (!isActive())
			return false;

		if (milestone) {
			addMilestone(mCurrentStep);
		}

		// If we're jumping forward, see if this is the milestone we
		// were looking for
		if (mJumpToNextMilestoneFlag) {
			if (milestone && mCurrentStep >= mMinimumMilestoneStep) {
				mJumpToNextMilestoneFlag = false;
				mTargetStep = mCurrentStep;
			} else {
				// Keep target just in front of current, so we
				// continue searching
				mTargetStep = Math.min(mTotalSteps, mCurrentStep + 1);
			}
		}

		if (mCurrentStep == mTargetStep) {

			// If the target step equals the total steps, we would have
			// expected to complete the algorithm without halting, so
			// the total steps is too small.
			// Increase the target step and total steps so that we end
			// up going all the way to the new end.
			if (!mCompleted && mTargetStep == mTotalSteps) {
				mTotalSteps++;
				mTargetStep++;
			} else {
				return true;
			}
		}
		ASSERT(mCurrentStep <= mTargetStep);
		mCurrentStep++;
		return false;
	}

	@Override
	public boolean bigStep() {
		return stepAux(true);
	}

	@Override
	public void show(String message, Renderable... renderables) {
		mFrameTitle = message;
		for (Renderable r : renderables) {
			mForegroundLayer.add(r);
		}
		throw new DesiredStepReachedException(message);
	}

	@Override
	public void setDoneMessage(String message) {
		if (!isActive())
			return;
		mDoneMessage = message;
	}

	/**
	 * Extract name of layer from key, if it exists. Examples:
	 * 
	 * "42 :   foo  " --> "foo"
	 * 
	 * "42" --> null
	 */
	private static String extractNameFromLayerKey(String key) {
		int colonPosition = key.indexOf(':');
		if (colonPosition < 0)
			return null;
		String name = key.substring(1 + colonPosition).trim();
		if (name.length() == 0)
			return null;
		return name;
	}

	@Override
	public void addLayer(String key, Renderable layer) {
		if (!isActive())
			return;
		// If this layer has a name that matches that of any existing
		// layer, don't open it. The name is defined as (trimmed) characters
		// following the first colon (:) in the layer's key
		String layerName = extractNameFromLayerKey(key);
		if (layerName != null) {
			if (mBackgroundLayerActiveNamesSet.contains(layerName)) {
				return;
			}
			mBackgroundLayerActiveNamesSet.add(layerName);
		}
		mBackgroundLayers.put(getScopedBackgroundLayerKey(key), layer);
	}

	/**
	 * Add a prefix to a background layer key that is unique to the current
	 * active stack depth
	 */
	private String getScopedBackgroundLayerKey(String key) {
		return d(mActiveStack.size(), 2) + "_" + key;
	}

	@Override
	public void removeLayer(String key) {
		if (!isActive())
			return;
		removeLayerAux(getScopedBackgroundLayerKey(key));
	}

	private void removeLayerAux(String key) {
		String layerName = extractNameFromLayerKey(key);
		if (layerName != null) {
			mBackgroundLayerActiveNamesSet.remove(layerName);
		}
		mBackgroundLayers.remove(key);
	}

	/**
	 * Remove any remaining background layers associated with the current active
	 * stack depth
	 */
	private void removeCurrentBackgroundLayers() {
		String currentPrefix = getScopedBackgroundLayerKey("");
		ArrayList<String> layersToRemove = new ArrayList();
		for (String key : mBackgroundLayers.keySet()) {
			if (key.startsWith(currentPrefix)) {
				layersToRemove.add(key);
			}
		}
		for (String key : layersToRemove)
			removeLayerAux(key);
	}

	public Renderable line(Point p1, Point p2) {
		return RenderTools.wrapRenderableWithState(new Segment(p1, p2));
	}

	@Override
	public Renderable highlightedLine(Point p1, Point p2) {
		RenderTools.setColorState(Color.RED);
		RenderTools.setLineWidthState(HIGHLIGHT_LINE_WIDTH);
		Renderable r = RenderTools.wrapRenderableWithState(new Segment(p1, p2));
		setNormal();
		return r;
	}

	@Override
	public void setColor(int color) {
		RenderTools.setColorState(color);
	}

	@Override
	public void setLineWidth(float lineWidth) {
		RenderTools.setLineWidthState(lineWidth);
	}

	@Override
	public void setNormal() {
		RenderTools.setColorState(Color.BLUE);
		RenderTools.setLineWidthState(1);
	}

	/**
	 * Construct a wrapper for a Renderable that renders it using highlighted
	 * colors
	 */
	public Renderable highlighted(final Renderable r) {
		return RenderTools.colored(Color.RED, r);
	}

	/**
	 * Construct a wrapper for a Renderable that renders it with a particular
	 * color
	 */
	public Renderable colored(final int color, final Renderable r) {
		return RenderTools.colored(color, r);
	}

	/**
	 * Request a refresh of the algorithm display. Runs to the target step (if
	 * possible) and displays that frame.
	 */
	public void refresh() {
		// If algorithm rectangle isn't prepared yet, ignore
		if (!isAlgorithmRectPrepared())
			return;
		// If we're currently doing a refresh, do nothing
		if (mRefreshing)
			return;
		mRefreshing = true;
		if (mOptions.getActiveAlgorithm() != null) {
			synchronized (getLock()) {
				acquireLock();
				performAlgorithm();
				releaseLock();
			}
		}
		mglSurfaceView.requestRender();
		mRefreshing = false;
	}

	private LinearLayout mAuxView;
	private View mStepperControlsView;

	/**
	 * Construct stepper controller view
	 */
	View buildControllerView() {
		mAuxView = UITools.linearLayout(mOptions.getContext(), true);

		AlgorithmStepperPanel panel = new AlgorithmStepperPanel(mOptions);
		mStepperControlsView = panel.view();

		return mAuxView;
	}

	/**
	 * Set contents of stepper view to either the stepper controls, or some
	 * other view
	 * 
	 * @param auxView
	 *            view, or null to use stepper controls
	 */
	void setAuxViewContent(View content) {
		if (content == null) {
			content = mStepperControlsView;
		}

		View currentContent = null;
		if (mAuxView.getChildCount() == 1)
			currentContent = mAuxView.getChildAt(0);
		if (currentContent != content) {
			mAuxView.removeAllViews();
			// Set layout parameters assuming we're adding at the bottom of a
			// vertical list
			mAuxView.addView(content, UITools.layoutParams(false));
		}
	}

	/**
	 * Render algorithm frame, by plotting all previously constructed layers and
	 * the frame's title
	 */
	void render() {
		String renderMessage = null;
		try {
			renderBackgroundElements();
			for (Renderable r : mForegroundLayer)
				r.render(this);
		} catch (Throwable t) {
			// Issue #99: don't obscure an existing message, which may be a
			// GeometryException
			renderMessage = "Problem while rendering...\n"
					+ constructExceptionString(t, "ConcreteStepper.render");
		}

		// Combine mFrameTitle and renderMessage to produce displayed title
		String title = mFrameTitle;
		if (renderMessage != null) {
			if (title != null)
				title = title + "\n\n" + renderMessage;
			else
				title = renderMessage;
		}
		if (title != null) {
			RenderTools.renderFrameTitle(title);
		}
	}

	/**
	 * Render persistent (background) elements, in order of sorted keys
	 */
	private void renderBackgroundElements() {
		ArrayList<String> keys = new ArrayList(mBackgroundLayers.keySet());
		Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
		for (String key : keys) {
			Renderable renderable = mBackgroundLayers.get(key);
			RenderTools.resetRenderStateVars();
			renderable.render(this);
		}
	}

	/**
	 * An exception of this type is thrown by the algorithm stepper when the
	 * target step is reached
	 */
	private static class DesiredStepReachedException extends RuntimeException {
		public DesiredStepReachedException(String message) {
			super(message);
		}
	}

	void calculateAlgorithmTotalSteps() {
		// Construct a stepper for this purpose, instead of this one
		TotalStepsCounter s = new TotalStepsCounter(this, mOptions);
		int totalSteps;
		synchronized (getLock()) {
			acquireLock();
			AlgorithmInput input = mEditor.constructAlgorithmInput();
			totalSteps = s.countSteps(input);
			// If the current target step is equal to its maximum, change it to
			// stick to the new maximum
			boolean atMax = (mOptions.readTargetStep() == mOptions
					.readTotalSteps());
			mOptions.setTotalSteps(totalSteps);
			if (atMax)
				mOptions.setTargetStep(totalSteps);
			releaseLock();
		}
	}

	private void performAlgorithm() {
		try {
			initializeActiveState(true);

			// Cache values from widgets to our temporary registers
			mTargetStep = mOptions.readTargetStep();
			mTotalSteps = mOptions.readTotalSteps();

			mCurrentStep = 0;

			mBackgroundLayers.clear();
			mBackgroundLayerActiveNamesSet.clear();
			mForegroundLayer.clear();
			mFrameTitle = null;
			mDoneMessage = "Done";

			RenderTools.resetRenderStateVars();

			mMilestones.clear();
			addMilestone(mCurrentStep);

			mCompleted = false;
			try {
				Algorithm algorithm = mOptions.getActiveAlgorithm();
				AlgorithmInput input = mEditor.constructAlgorithmInput();
				algorithm.run(this, input);

				// We completed the algorithm without halting.
				// We're about to throw an exception that will be caught below;
				// set flag so that we know we completed without halting.
				mCompleted = true;

				// If the target step was not the maximum, the maximum is too
				// high.
				if (mCurrentStep < mTotalSteps) {
					mTotalSteps = mCurrentStep;
					mTargetStep = mTotalSteps;
				}
				// Always end an algorithm with a step/show combination
				if (bigStep()) { // should always return true
					show(mDoneMessage);
				} else {
					die("unexpected!");
				}
			} catch (DesiredStepReachedException e) {
				if (!mCompleted) {
					// We halted without completing. If we halted on what we
					// thought was the last step, the total steps is too low.
					if (mCurrentStep == mTotalSteps) {
						mTotalSteps = (int) (Math.max(mTotalSteps, 50) * 1.3f);
					}
				}
				throw e;
			} catch (RuntimeException t) {
				// Pop active stack until it's empty; we want to make sure
				// this message gets displayed, even if it occurred during a
				// sequence for which stepping is disabled
				while (!mActiveStack.isEmpty())
					popActive();
				mTotalSteps = mCurrentStep;
				mTargetStep = mCurrentStep;
				show(constructExceptionString(t,
						"ConcreteStepper.performAlgorithm"));
			}
		} catch (DesiredStepReachedException e) {
			// Write cached values back to widgets
			mOptions.setTotalSteps(mTotalSteps);
			mOptions.setTargetStep(mTargetStep);
		} finally {
			mJumpToNextMilestoneFlag = false;
			initializeActiveState(false);
		}
	}

	/**
	 * Extract a stack trace from a throwable as a string
	 * 
	 * @param throwable
	 *            throwable
	 * @param haltPrefix
	 *            if a stack trace entry has this prefix, omits it and all
	 *            subsequent entries
	 */
	private String constructExceptionString(Throwable throwable,
			String haltPrefix) {
		StringBuilder sb = new StringBuilder();
		sb.append(throwable.getClass().getSimpleName());
		sb.append("; ");
		String message = throwable.getMessage();
		if (message != null)
			sb.append(message);

		String trace = stackTrace(throwable);
		for (String entry : trace.split("\\n")) {
			if (entry.startsWith(haltPrefix)) {
				break;
			}
			sb.append('\n');
			sb.append(entry);
		}
		return sb.toString();
	}

	private void adjustTargetMilestone(int delta) {
		int seekStep = -1;
		int targetStep = mOptions.readTargetStep();
		if (delta < 0) {
			for (int k : mMilestones) {
				if (k < targetStep)
					seekStep = k;
			}
		} else {
			// Act as if we're just stepping forward by one, but set a special
			// flag which indicates we want to continue stepping forward until
			// we reach a milestone
			int totalSteps = mOptions.readTotalSteps();
			seekStep = Math.min(totalSteps, targetStep + 1);
			// We must be careful to only set the 'jump to next' flag if we're
			// actually going to perform any stepping, otherwise it won't get
			// cleared
			if (seekStep > targetStep) {
				mJumpToNextMilestoneFlag = true;
				mMinimumMilestoneStep = seekStep;
			}
		}
		if (seekStep >= 0) {
			mOptions.setTargetStep(seekStep);
		}
	}

	private void addMilestone(int n) {
		int lastMilestone = -1;
		if (!mMilestones.isEmpty()) {
			lastMilestone = last(mMilestones);
		}
		if (n != lastMilestone)
			mMilestones.add(n);
	}

	void addStepperViewListeners() {

		final String[] ids = { WIDGET_ID_JUMP_BWD, WIDGET_ID_JUMP_FWD,
				WIDGET_ID_STEP_BWD, WIDGET_ID_STEP_FWD };

		AbstractWidget.Listener listener = new AbstractWidget.Listener() {
			@Override
			public void valueChanged(AbstractWidget widget) {
				String id = widget.getId();
				for (int j = 0; j < 2; j++) {
					if (id == ids[j])
						adjustTargetMilestone(j == 0 ? -1 : 1);
					if (id == ids[j + 2]) {
						int seekStep = mOptions.readTargetStep()
								+ (j == 0 ? -1 : 1);
						seekStep = MyMath.clamp(seekStep, 0,
								mOptions.readTotalSteps());
						mOptions.setTargetStep(seekStep);
					}
				}
			}
		};
		for (int i = 0; i < ids.length; i++) {
			mOptions.getWidget(ids[i]).addListener(listener);
		}
	}

	/**
	 * Get the object that serves as the synchronization lock to avoid race
	 * conditions between the UI and OpenGL threads. This method should only be
	 * invoked as part of the following structure:
	 * 
	 * <pre>
	 * 
	 * synchronized(getLock()) { 
	 *     acquireLock();
	 *       ...
	 *       ...
	 *     releaseLock();
	 * }
	 * 
	 * </pre>
	 * 
	 * The calls to acquireLock() and releaseLock() do nothing if VERIFY_LOCK is
	 * false.
	 * 
	 */
	public Object getLock() {
		return aSynchronizationLock;
	}

	public void acquireLock() {
		if (VERIFY_LOCK) {
			if (aLockCounter != 0
					&& Thread.currentThread() != aLockActiveThread) {
				lockError();
			}
			aLockCounter++;
			aLockActiveThread = Thread.currentThread();
			aLockAquireInfo = stackTrace(1, 3, null);
		}
	}

	public void releaseLock() {
		if (VERIFY_LOCK) {
			haveLock();
			aLockCounter--;
			if (aLockCounter == 0) {
				aLockActiveThread = null;
				aLockAquireInfo = null;
			}
		}
	}

	private void lockError() {
		if (VERIFY_LOCK) {
			die("unexpected lock thread " + nameOf(Thread.currentThread())
					+ " vs " + nameOf(aLockActiveThread) + "; acquired:\n"
					+ aLockAquireInfo);
		}
	}

	void haveLock() {
		if (VERIFY_LOCK) {
			if (Thread.currentThread() != aLockActiveThread) {
				lockError();
			}
		}
	}

	void begin() {
		if (mAlgorithms.isEmpty())
			die("no algorithms specified");

		// This synchronization is probably not necessary, but to satisfy the
		// VERIFY_LOCK calls we need it.
		synchronized (getLock()) {
			if (VERIFY_LOCK)
				warning("VERIFY_LOCK flag is true");
			acquireLock();
			mOptions.begin(mAlgorithms);
			releaseLock();
		}
	}

	/**
	 * Set rendering state. If false, any render operations will generate
	 * display elements for later rendering; if true, render operations actually
	 * perform the rendering.
	 * 
	 * If the value is changing, resets the rendering state variables.
	 */
	public void setRendering(boolean f) {
		if (mRendering != f) {
			mRendering = f;
			RenderTools.resetRenderStateVars();
		}
	}

	boolean rendering() {
		return mRendering;
	}

	boolean isAlgorithmRectPrepared() {
		return mAlgorithmRect != null;
	}

	private Object aSynchronizationLock = new Object();
	private int aLockCounter;
	private Thread aLockActiveThread;
	private String aLockAquireInfo;

	private AlgorithmOptions mOptions;
	private Editor mEditor;
	private List<Algorithm> mAlgorithms = new ArrayList();
	private List<Renderable> mForegroundLayer = new ArrayList();
	private Map<String, Renderable> mBackgroundLayers = new HashMap();
	// Map of layer names -> existing layer sharing that name
	private Set<String> mBackgroundLayerActiveNamesSet = new HashSet();
	private String mFrameTitle;
	private String mDoneMessage;
	private List<Integer> mMilestones = new ArrayList();
	private Rect mAlgorithmRect;
	private Rect mVisibleRect;
	private boolean mCompleted;
	private GLSurfaceView mglSurfaceView;
	private boolean mRendering;

	// True if jumping forward to next milestone;
	private boolean mJumpToNextMilestoneFlag;
	// stop at first milestone whose step is at least this
	private int mMinimumMilestoneStep;
	// flag to indicate whether refresh() is occurring
	private boolean mRefreshing;

	// For efficiency, cached values of target / total steps widgets used only
	// during performAlgorithm()
	private int mTargetStep;
	private int mTotalSteps;
	private int mCurrentStep;
}
