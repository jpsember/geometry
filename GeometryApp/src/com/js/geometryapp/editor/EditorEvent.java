package com.js.geometryapp.editor;

import com.js.geometry.Point;
import static com.js.basic.Tools.*;

@Deprecated
public class EditorEvent {

	public static final int CODE_NONE = 0;

	// single touch events
	public static final int CODE_DOWN = 1;
	public static final int CODE_DRAG = 2;
	public static final int CODE_UP = 3;

	// stop existing operation, if one is occurring
	public static final int CODE_STOP = 4;

	public static final EditorEvent NONE = new EditorEvent(CODE_NONE);
	public static final EditorEvent STOP = new EditorEvent(CODE_STOP);

	public EditorEvent(int code, Point location, boolean multipleTouchFlag) {
		mCode = code;
		mLocation = location;
		mMultipleTouchFlag = multipleTouchFlag;
	}

	public EditorEvent(int code, Point location) {
		mCode = code;
		mLocation = location;
	}

	public EditorEvent(int code) {
		this(code, null, false);
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
		return mCode == CODE_DOWN;
	}

	public boolean isUpVariant() {
		return mCode == CODE_UP;
	}

	public boolean isDragVariant() {
		return mCode == CODE_DRAG;
	}

	public boolean hasLocation() {
		return mLocation != null;
	}

	public boolean isMultipleTouch() {
		if (!hasLocation())
			throw new IllegalStateException();
		return mMultipleTouchFlag;
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

	private static String sEditorEventNames[] = { "NONE", "DOWN", "DRAG",
			"UP  ", "STOP", };

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
	private boolean mMultipleTouchFlag;
}
