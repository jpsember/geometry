package com.js.geometry;

import static com.js.basic.Tools.pop;

import java.util.ArrayList;
import java.util.List;

import com.js.geometryapp.Algorithm;

public abstract class AlgorithmStepper {

	private static Rect sAlgorithmRect = new Rect(0, 0, 1200, 1000);
	protected static final String EMPTY_STRING = "";

	/**
	 * A stepper that does nothing
	 */
	public static final AlgorithmStepper INACTIVE_STEPPER = new InactiveStepper();

	/**
	 * Add an algorithm. Each algorithm will appear in its own options panel
	 */
	public void addAlgorithm(Algorithm delegate) {
	}

	/**
	 * Get rectangle defining the algorithm's logical bounds. Elements outside
	 * of this rectangle may not be visible within the algorithm view
	 */
	public Rect algorithmRect() {
		return sAlgorithmRect;
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

	protected void initializeActiveState(boolean active) {
		setActive(active);
		mActiveStack.clear();
	}

	/**
	 * Push active state to value of checkbox widget
	 * 
	 * @param widgetId
	 *            id of checkbox widget to read
	 */
	public void pushActive(String widgetId) {
	}

	/**
	 * Pop stepper active state previously pushed via a pushActive call
	 */
	public void popActive() {
		if (mActiveStack.isEmpty())
			throw new IllegalStateException("active stack is empty");
		mActive = pop(mActiveStack);
	}

	/**
	 * Determine if we should stop and display this frame of the current
	 * algorithm; should be followed by a call to show() if this returns true.
	 * 
	 * If the stepper is not active, returns false. Otherwise, The current step
	 * will be incremented iff this method returns false.
	 */
	public boolean step() {
		return false;
	}

	/**
	 * Perform step(), but with a step that is a milestone
	 */
	public boolean bigStep() {
		return false;
	}

	/**
	 * Generate an algorithm step. For efficiency, should only be called if
	 * step() returned true.
	 * 
	 * Sets the frame title to the message, and throws an exception to halt the
	 * algorithm.
	 * 
	 * @param message
	 *            message to display, which may cause other elements to be
	 *            displayed via side effects
	 */
	public void show(String message) {
	}

	/**
	 * Specify a message to display when the algorithm completes, in lieu of a
	 * generic one. Has no effect if stepper is not active
	 * 
	 * @param message
	 *            message to display; note that unlike calls to show(), this
	 *            will not cause any elements displayed via side effects (in
	 *            other words, don't concatenate 'plot' method calls to this
	 *            one)
	 */
	public void setDoneMessage(String message) {
	}

	/**
	 * Add a Renderable as a layer: one that will appear in every rendered frame
	 * (until the layer is removed). Layers are plotted in alphabetical order by
	 * key, so the last layer plotted is topmost in the view.
	 * 
	 * If the layer has a name, and the name matches that of any existing layer,
	 * it won't be added. The name is defined as (trimmed) characters following
	 * the first colon (:) in the layer's key. For example, to prevent a
	 * particular mesh from being drawn twice: once in an outer algorithm, and
	 * again within a call to a subalgorithm.
	 * 
	 * @param key
	 *            uniquely distinguishes this layer from others; can optionally
	 *            contain a name suffix ":xxx"
	 */
	public void addLayer(String key, Renderable renderable) {
	}

	/**
	 * Remove a layer, so it will no longer be plotted
	 */
	public void removeLayer(String key) {
	}

	/**
	 * Set color for subsequent render operations
	 * 
	 * @return an empty string, as a convenience so calls to this class's
	 *         methods can be chained together to form a single show() method
	 *         argument
	 */
	public String setColor(int color) {
		return EMPTY_STRING;
	}

	/**
	 * Set line width for subsequent render operations
	 */
	public String setLineWidth(float lineWidth) {
		return EMPTY_STRING;
	}

	/**
	 * Add a Renderable element to be displayed with this algorithm frame
	 */
	public String plot(Renderable element) {
		return EMPTY_STRING;
	}

	/**
	 * Like plot(), but renders in a highlighted state. The render state (color,
	 * line width) is reset to default values after this call
	 */
	public String highlight(Renderable element) {
		return EMPTY_STRING;
	}

	/**
	 * Convenience method equivalent to plot(new Segment(p1,p2))
	 */
	public String plotLine(Point p1, Point p2) {
		return EMPTY_STRING;
	}

	/**
	 * Convenience method equivalent to highlight(new Segment(p1,p2))
	 */
	public String highlightLine(Point p1, Point p2) {
		return EMPTY_STRING;
	}

	/**
	 * Restore default rendering attributes: color = BLUE, line width = 1
	 */
	public String setNormal() {
		return EMPTY_STRING;
	}

	/**
	 * Construct a wrapper for a Renderable that renders it using highlighted
	 * colors; default implementation returns the original renderable
	 */
	public Renderable highlighted(final Renderable r) {
		return r;
	}

	/**
	 * Construct a wrapper for a Renderable that renders it with a particular
	 * color; default implementation returns the original renderable
	 */
	public Renderable colored(final int color, final Renderable r) {
		return r;
	}

	private void setActive(boolean active) {
		mActive = active;
	}

	private boolean mActive;
	private List<Boolean> mActiveStack = new ArrayList();
}
