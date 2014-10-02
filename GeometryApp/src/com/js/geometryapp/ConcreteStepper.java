package com.js.geometryapp;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.view.View;

import com.js.android.MyActivity;
import com.js.geometry.Edge;
import com.js.geometry.GeometryException;
import com.js.geometry.Mesh;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometryapp.widget.AbstractWidget;

class ConcreteStepper implements AlgorithmStepper {

	static final String WIDGET_ID_JUMP_BWD = "<<";
	static final String WIDGET_ID_JUMP_FWD = ">>";
	static final String WIDGET_ID_STEP_BWD = "<";
	static final String WIDGET_ID_STEP_FWD = ">";

	private static final float HIGHLIGHT_LINE_WIDTH = 3.0f;

	ConcreteStepper() {
	}

	void setGLSurfaceView(GLSurfaceView glSurfaceView) {
		mglSurfaceView = glSurfaceView;
	}

	@Override
	public void addAlgorithm(Algorithm delegate) {
		mAlgorithms.add(delegate);
	}

	@Override
	public Rect algorithmRect() {
		if (mAlgorithmRect == null) {
			DisplayMetrics m = MyActivity.displayMetrics();
			if (m.widthPixels > m.heightPixels) {
				mAlgorithmRect = new Rect(0, 0, 1200, 1000);
			} else {
				mAlgorithmRect = new Rect(0, 0, 1000, 1200);
			}
		}
		return mAlgorithmRect;
	}

	@Override
	public boolean isActive() {
		return mActive;
	}

	@Override
	public void pushActive(boolean active) {
		mActiveStack.add(mActive);
		mActive &= active;
	}

	@Override
	public void popActive() {
		if (mActiveStack.isEmpty())
			throw new IllegalStateException("active stack is empty");
		mActive = pop(mActiveStack);
	}

	@Override
	public void pushActive(String widgetId) {
		boolean value = mActive;
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
		if (!mCalculatingTotalSteps) {

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
		}
		mCurrentStep++;
		return false;
	}

	@Override
	public boolean bigStep() {
		return stepAux(true);
	}

	@Override
	public void show(String message) {
		ASSERT(!mCalculatingTotalSteps);
		mFrameTitle = message;
		throw new DesiredStepReachedException(message);
	}

	private void assertActive() {
		if (!isActive())
			throw new IllegalStateException("stepper must be active");
	}

	@Override
	public boolean openLayer(String key) {
		if (!(isActive() && !mCalculatingTotalSteps))
			return false;

		if (mActiveBackgroundLayer != null)
			throw new IllegalStateException("layer already open");
		AlgorithmDisplayElement.resetRenderStateVars();
		mActiveBackgroundLayer = new Layer(key);
		mBackgroundLayers.put(key, mActiveBackgroundLayer);

		return true;
	}

	@Override
	public void closeLayer() {
		assertActive();
		AlgorithmDisplayElement.resetRenderStateVars();
		mActiveBackgroundLayer = null;
	}

	@Override
	public void removeLayer(String key) {
		assertActive();
		mBackgroundLayers.remove(key);
	}

	@Override
	public String plot(AlgorithmDisplayElement element) {
		do {
			if (AlgorithmDisplayElement.rendering()) {
				element.render();
				break;
			}

			// Don't modify any layers if we're just calculating total steps
			ASSERT(!mCalculatingTotalSteps);

			// If there's an active background layer, add it to that instead
			Layer targetLayer = mActiveBackgroundLayer;
			if (targetLayer == null)
				targetLayer = mForegroundLayer;
			targetLayer.add(element);

		} while (false);
		return "";
	}

	@Override
	public String plot(Point point) {
		return plot(point, 1);
	}

	@Override
	public String highlight(Point point) {
		return setColor(Color.RED) + plot(point) + setNormal();
	}

	@Override
	public String plot(Point point, float radius) {
		return plot(new PointElement(point, radius));
	}

	@Override
	public String highlight(Point point, float radius) {
		return plot(new PointElement(point, radius));
	}

	@Override
	public String plotRay(Point p1, Point p2) {
		return plot(new RayElement(p1, p2));
	}

	@Override
	public String plot(Edge edge) {
		return plotRay(edge.sourceVertex(), edge.destVertex());
	}

	@Override
	public String highlightRay(Point p1, Point p2) {
		return setColor(Color.RED) + setLineWidth(HIGHLIGHT_LINE_WIDTH)
				+ plotRay(p1, p2) + setNormal();
	}

	@Override
	public String highlight(Edge edge) {
		return highlightRay(edge.sourceVertex(), edge.destVertex());
	}

	@Override
	public String plotLine(Point p1, Point p2) {
		return plot(new LineElement(p1, p2));
	}

	@Override
	public String highlightLine(Point p1, Point p2) {
		return setColor(Color.RED) + setLineWidth(HIGHLIGHT_LINE_WIDTH)
				+ plotLine(p1, p2) + setNormal();
	}

	@Override
	public String plot(Polygon polygon) {
		return plot(polygon, false);
	}

	@Override
	public String plot(Polygon polygon, boolean filled) {
		return plot(new PolygonElement(polygon,
				filled ? PolygonElement.Style.FILLED
						: PolygonElement.Style.BOUNDARY));
	}

	@Override
	public String highlight(Polygon polygon, boolean filled) {
		return setColor(Color.RED) + setLineWidth(HIGHLIGHT_LINE_WIDTH)
				+ plot(polygon, filled) + setNormal();
	}

	@Override
	public String plotPolyline(Collection<Point> endpoints) {
		return plot(new PolygonElement(new Polygon(endpoints),
				PolygonElement.Style.POLYLINE));
	}

	@Override
	public String highlightPolyline(Collection<Point> endpoints) {
		return setColor(Color.RED) + setLineWidth(HIGHLIGHT_LINE_WIDTH)
				+ plotPolyline(endpoints) + setNormal();
	}

	@Override
	public String plotMesh(Mesh meshContext) {
		return plot(new MeshElement(meshContext));
	}

	@Override
	public String setColor(int color) {
		AlgorithmDisplayElement.setColorState(color);
		return "";
	}

	@Override
	public String setLineWidth(float lineWidth) {
		AlgorithmDisplayElement.setLineWidthState(lineWidth);
		return "";
	}

	@Override
	public String setNormal() {
		return setColor(Color.BLUE) + setLineWidth(1);
	}

	/**
	 * Request a refresh of the algorithm display. Runs to the target step (if
	 * possible) and displays that frame.
	 * 
	 * @param widget
	 *            for test purposes only; the widget that induced this call, or
	 *            null
	 */
	void refresh(AbstractWidget widget) {
		if (db)
			pr("refresh due to " + widget);

		// If we're currently doing a refresh, do nothing
		if (mRefreshing) {
			if (db)
				pr("...already doing refresh, ignoring;\n" + stackTrace(0, 18));
			return;
		}
		mRefreshing = true;
		synchronized (getLock()) {
			performAlgorithm();
			mglSurfaceView.requestRender();
		}
		mRefreshing = false;
	}

	/**
	 * Construct stepper controller view
	 */
	View controllerView() {
		return AlgorithmStepperPanel.build(mOptions);
	}

	AlgorithmOptions constructOptions(Context context) {
		mOptions = new AlgorithmOptions(context, this);
		return mOptions;
	}

	/**
	 * Render algorithm frame, by plotting all previously constructed layers and
	 * the frame's title
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

	void calculateAlgorithmTotalSteps() {
		if (true) {
			warning("disabled temporarily");
			return;
		}
		mCalculatingTotalSteps = true;
		performAlgorithm();
	}

	private void performAlgorithm() {
		synchronized (getLock()) {
			try {
				initializeActiveState(true);

				// Cache values from widgets to our temporary registers
				mTargetStep = mOptions.readTargetStep();
				mTotalSteps = mOptions.readTotalSteps();

				mCurrentStep = 0;

				if (!mCalculatingTotalSteps) {
					mActiveBackgroundLayer = null;
					mBackgroundLayers.clear();
					mForegroundLayer.clear();
					mFrameTitle = null;
				}

				AlgorithmDisplayElement.resetRenderStateVars();

				mMilestones.clear();
				addMilestone(mCurrentStep);

				mCompleted = false;
				try {
					mOptions.runActiveAlgorithm();

					// We completed the algorithm without halting.

					// We're about to throw an exception that will be caught
					// below; set flag so that we know we completed without
					// halting.
					mCompleted = true;

					if (mCalculatingTotalSteps) {
						mTotalSteps = mCurrentStep;
						mTargetStep = MyMath.clamp(mTargetStep, 0, mTotalSteps);
						// Generate final milestone, and increment the step
						bigStep();
						// don't change the displayed message; just throw...
						throw new DesiredStepReachedException("(done calc)");
					}

					// If the target step was not the maximum, the maximum is
					// too high.
					if (mCurrentStep < mTotalSteps) {
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
					if (!mCalculatingTotalSteps) {
						if (!mCompleted) {
							// We halted without completing. If we halted on
							// what we
							// thought was the last step, the total steps is too
							// low.
							if (mCurrentStep == mTotalSteps) {
								mTotalSteps = (int) (Math.max(mTotalSteps, 50) * 1.3f);
							}
						}
					}
					throw e;
				} catch (RuntimeException t) {
					// Pop active stack until it's empty; we want to make sure
					// this message gets displayed, even if it occurred during a
					// sequence for which stepping is disabled
					while (!mActiveStack.isEmpty())
						popActive();

					String description = t.toString() + "\n" + stackTrace(t);
					if (!description.equals(mPreviousExceptionDescription)) {
						pr(description);
						mPreviousExceptionDescription = description;
					}
					if (!(t instanceof GeometryException))
						throw t;

					mTotalSteps = mCurrentStep;
					mTargetStep = MyMath.clamp(mTargetStep, 0, mTotalSteps);
					show("Caught: " + t);
				}
			} catch (DesiredStepReachedException e) {
				// Write cached values back to widgets
				mOptions.setTotalSteps(mTotalSteps);
				mOptions.setTargetStep(mTargetStep);
			} finally {
				mJumpToNextMilestoneFlag = false;
				mCalculatingTotalSteps = false;
				initializeActiveState(false);
			}
		}
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

	private void initializeActiveState(boolean active) {
		mActive = active;
		mActiveStack.clear();
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
	 * conditions between the UI and OpenGL threads
	 * 
	 * @return
	 */
	Object getLock() {
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

	void begin() {
		if (mAlgorithms.isEmpty())
			die("no algorithms specified");
		mOptions.begin(mAlgorithms);
		refresh(null);
	}

	private Object sSynchronizationLock = new Object();
	private AlgorithmOptions mOptions;
	private ArrayList<Algorithm> mAlgorithms = new ArrayList();
	private Layer mForegroundLayer = new Layer("_");
	private Map<String, Layer> mBackgroundLayers = new HashMap();
	private String mFrameTitle;
	private ArrayList<Integer> mMilestones = new ArrayList();
	private boolean mActive;
	private ArrayList<Boolean> mActiveStack = new ArrayList();
	private Layer mActiveBackgroundLayer;
	private Rect mAlgorithmRect;
	private boolean mCompleted;
	private GLSurfaceView mglSurfaceView;

	// True if jumping forward to next milestone;
	private boolean mJumpToNextMilestoneFlag;
	// stop at first milestone whose step is at least this
	private int mMinimumMilestoneStep;
	// True if performing algorithm just to calculate total steps
	private boolean mCalculatingTotalSteps;
	// flag to indicate whether refresh() is occurring
	private boolean mRefreshing;

	// For efficiency, cached values of target / total steps widgets used only
	// during performAlgorithm()
	private int mTargetStep;
	private int mTotalSteps;
	private int mCurrentStep;

	private String mPreviousExceptionDescription;

}
