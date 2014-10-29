package com.js.geometry;

import com.js.geometryapp.Algorithm;

public interface AlgorithmStepper {

	/**
	 * A stepper that does nothing
	 */
	public static final AlgorithmStepper DEFAULT_STEPPER = new DefaultStepper();

	/**
	 * Add an algorithm. Each algorithm will appear in its own options panel
	 */
	public void addAlgorithm(Algorithm delegate);

	/**
	 * Get rectangle defining the algorithm's logical bounds. Elements outside
	 * of this rectangle may not be visible within the algorithm view
	 */
	public Rect algorithmRect();

	/**
	 * Determine if algorithm stepper is active (i.e. hooked up to a control
	 * panel and controlling the progress of the calling algorithm)
	 */
	public boolean isActive();

	/**
	 * Save current stepper active state on a stack, and push a new value (which
	 * is AND'd with previous state)
	 */
	public void pushActive(boolean active);

	/**
	 * Push active state to value of checkbox widget
	 * 
	 * @param widgetId
	 *            id of checkbox widget to read
	 */
	public void pushActive(String widgetId);

	/**
	 * Pop stepper active state previously pushed via a pushActive call
	 */
	public void popActive();

	/**
	 * Determine if we should stop and display this frame of the current
	 * algorithm; should be followed by a call to show() if this returns true.
	 * 
	 * If the stepper is not active, returns false. Otherwise, The current step
	 * will be incremented iff this method returns false.
	 */
	public boolean step();

	/**
	 * Perform step(), but with a step that is a milestone
	 */
	public boolean bigStep();

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
	public void show(String message);

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
	public void setDoneMessage(String message);

	/**
	 * Request to open a background layer. Subsequent plot() commands will be
	 * redirected to this layer. If this returns true, then must be balanced by
	 * a call to closeLayer(). Layers are plotted in alphabetical order, so the
	 * last layer plotted is topmost in the view. Once defined, layers will
	 * appear in every rendered frame, in addition to step-specific elements
	 * 
	 * @param key
	 *            uniquely distinguishes this layer from others
	 */
	public boolean openLayer(String key);

	/**
	 * Close layer previously opened via openLayer()
	 */
	public void closeLayer();

	/**
	 * Remove a layer, so it will no longer be plotted
	 */
	public void removeLayer(String key);

	/**
	 * Set color for subsequent render operations
	 * 
	 * @return an empty string, as a convenience so calls to this class's
	 *         methods can be chained together to form a single show() method
	 *         argument
	 */
	public String setColor(int color);

	/**
	 * Set line width for subsequent render operations
	 */
	public String setLineWidth(float lineWidth);

	/**
	 * Add a Renderable element to be displayed with this algorithm frame
	 * 
	 * @param element
	 */
	public String plot(Renderable element);

	/**
	 * Like plot(), but renders in a highlighted state. The render state (color,
	 * line width) is reset to default values after this call
	 */
	public String highlight(Renderable element);

	/**
	 * Convenience method equivalent to plot(new Segment(p1,p2))
	 */
	public String plotLine(Point p1, Point p2);

	/**
	 * Convenience method equivalent to highlight(new Segment(p1,p2))
	 */
	public String highlightLine(Point p1, Point p2);

	public String plotSprite(int spriteResourceId, Point location);

	public String highlightSprite(int spriteResourceId, Point location);

	/**
	 * Restore default rendering attributes: color = BLUE, line width = 1
	 */
	public String setNormal();

}
