package com.js.geometryapp.editor;

import com.js.geometry.Point;

public interface EditEventListener {

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
	// start operation to add new item
	public static final int EVENT_ADD_NEW = 8;

	public int processEvent(int eventCode, Point location);
}
