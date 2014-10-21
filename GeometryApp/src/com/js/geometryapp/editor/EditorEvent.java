package com.js.geometryapp.editor;

import static com.js.basic.Tools.DEBUG_ONLY_FEATURES;
import static com.js.basic.Tools.pr;

import com.js.geometry.Point;
import static com.js.basic.Tools.*;

public class EditorEvent {

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

	public static final EditorEvent NONE = new EditorEvent(EVENT_NONE);
	public static final EditorEvent STOP = new EditorEvent(EVENT_STOP);

	public EditorEvent(int code, Point location) {
		mCode = code;
		mLocation = location;
	}

	public EditorEvent(int code) {
		this(code, null);
	}

	public int getCode() {
		return mCode;
	}

	public Point getLocation() {
		if (!hasLocation())
			throw new IllegalStateException();
		return mLocation;
	}

	public boolean isDownVariant() {
		return mCode == EVENT_DOWN || mCode == EVENT_DOWN_MULTIPLE;
	}

	public boolean isUpVariant() {
		return mCode == EVENT_UP || mCode == EVENT_UP_MULTIPLE;
	}

	public boolean isDragVariant() {
		return mCode == EVENT_DRAG || mCode == EVENT_DRAG_MULTIPLE;
	}

	public boolean hasLocation() {
		return mLocation != null;
	}

	public void printProcessingMessage(String message) {
		if (!DEBUG_ONLY_FEATURES)
			return;
		else {
			if (isDragVariant() && getCode() == sPreviousPrintEvent.getCode()
					&& message.equals(sPreviousPrintMessage))
				return;
			pr(message + "; processing:   " + this);
			sPreviousPrintEvent = this;
			sPreviousPrintMessage = message;
		}
	}

	private static String sEditorEventNames[] = { "NONE", "DOWN", "DRAG", "UP",
			"DOWN_M", "DRAG_M", "UP_M", "STOP", };

	public static String editorEventName(int eventCode) {
		if (!DEBUG_ONLY_FEATURES)
			return null;
		if (eventCode < 0 || eventCode >= sEditorEventNames.length)
			return "??#" + eventCode + "??";
		return sEditorEventNames[eventCode];
	}

	@Override
	public String toString() {
		if (!DEBUG_ONLY_FEATURES)
			return super.toString();
		StringBuilder sb = new StringBuilder();
		if (mCode < 0 || mCode >= sEditorEventNames.length) {
			sb.append("??#" + mCode + "??");
		} else {
			sb.append(sEditorEventNames[mCode]);
		}
		tab(sb, 10);
		if (hasLocation()) {
			sb.append(" ");
			sb.append(getLocation());
		}
		return sb.toString();
	}

	private static EditorEvent sPreviousPrintEvent = NONE;
	private static String sPreviousPrintMessage;

	private int mCode;
	private Point mLocation;

}
