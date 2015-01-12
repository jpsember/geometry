package com.js.editor;

import com.js.geometry.IPoint;
import com.js.geometry.Point;
import static com.js.basic.Tools.*;

/**
 * <pre>
 * 
 * Represents a user-generated event to be manipulated by an editor.
 * 
 * The user generates events. 
 * 
 * These events are processed by the current operation.
 * 
 * An operation issues commands which modifies the state of the 
 * object being edited.
 * 
 * </pre>
 */
public class UserEvent {

  public static final int CODE_NONE = 0;

  // single touch events
  public static final int CODE_DOWN = 1;
  public static final int CODE_DRAG = 2;
  public static final int CODE_UP = 3;

  // stop existing operation, if one is occurring
  public static final int CODE_STOP = 4;

  public static final UserEvent NONE = new UserEvent(CODE_NONE);
  public static final UserEvent STOP = new UserEvent(CODE_STOP);
  public static final int FLAG_RIGHT = (1 << 0);
  public static final int FLAG_CTRL = (1 << 1);
  public static final int FLAG_SHIFT = (1 << 2);
  public static final int FLAG_ALT = (1 << 3);
  public static final int FLAG_META = (1 << 4);

  public UserEvent(int code, UserEventSource source, IPoint viewLocation,
      int modifierFlags) {
    mCode = code;
    mSource = source;
    mViewLocation = viewLocation;
    mModifierFlags = modifierFlags;
  }

  public UserEvent(int code) {
    mCode = code;
  }

  public IPoint getViewLocation() {
    if (!hasLocation())
      throw new IllegalStateException();
    return mViewLocation;
  }

  public int getCode() {
    return mCode;
  }

  public Point getWorldLocation() {
    if (!hasLocation())
      throw new IllegalStateException();
    if (mWorldLocation == null) {
      mWorldLocation = mSource.viewToWorld(mViewLocation.toPoint());
    }
    return mWorldLocation;
  }

  public UserEventSource getSource() {
    return mSource;
  }

  public UserEventManager getManager() {
    return getSource().getManager();
  }

  /**
   * Convenience method for getManager().setOperation()
   */
  public void setOperation(UserOperation oper) {
    getManager().setOperation(oper);
  }

  /**
   * Convenience method for getManager().clearOperation()
   */
  public void clearOperation() {
    getManager().clearOperation();
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
    return mViewLocation != null;
  }

  public boolean isRight() {
    return hasFlag(FLAG_RIGHT);
  }

  public boolean isAlt() {
    return hasFlag(FLAG_ALT);
  }

  public boolean isMeta() {
    return hasFlag(FLAG_META);
  }

  public boolean isCtrl() {
    return hasFlag(FLAG_CTRL);
  }

  public boolean isShift() {
    return hasFlag(FLAG_SHIFT);
  }

  public boolean hasModifierKeys() {
    return hasFlag(FLAG_CTRL | FLAG_SHIFT | FLAG_ALT | FLAG_META);
  }

  private boolean hasFlag(int f) {
    return 0 != (mModifierFlags & f);
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

  private static String sEditorEventNames[] = { "NONE", "DOWN", "DRAG", "UP  ",
      "STOP", };

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
      sb.append(" w:");
      sb.append(getWorldLocation());
      sb.append(" v:");
      sb.append(getViewLocation());
      if (mModifierFlags != 0) {
        sb.append(" <");
        if (isAlt())
          sb.append("A");
        if (isCtrl())
          sb.append("C");
        if (isMeta())
          sb.append("M");
        if (isRight())
          sb.append("R");
        if (isShift())
          sb.append("S");
        sb.append(">");
      }
    }
    return sb.toString();
  }

  public static interface Listener {
    public void processUserEvent(UserEvent event);
  }

  private static UserEvent sPreviousPrintEvent = NONE;
  private static String sPreviousPrintMessage;

  private int mCode;
  private UserEventSource mSource;
  private IPoint mViewLocation;
  private Point mWorldLocation;
  private boolean mMultipleTouchFlag;
  private int mModifierFlags;
}
