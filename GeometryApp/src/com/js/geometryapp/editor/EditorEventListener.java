package com.js.geometryapp.editor;

import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;

public interface EditorEventListener {

	public static final int EVENT_NONE = 0;

	// single touch events
	public static final int EVENT_DOWN = 1;
	public static final int EVENT_DRAG = 2;
	public static final int EVENT_UP = 3;

	// multiple touch events
	public static final int EVENT_DOWN_MULTIPLE = 4;
	public static final int EVENT_DRAG_MULTIPLE = 5;
	public static final int EVENT_UP_MULTIPLE = 6;

	// stop existing operation, if one is occurring
	public static final int EVENT_STOP = 7;

	public int processEvent(int eventCode, Point location);

	// Perform any rendering specific to this operation
	public void render(AlgorithmStepper s);
}
