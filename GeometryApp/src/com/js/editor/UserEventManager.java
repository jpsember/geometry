package com.js.editor;

import static com.js.basic.Tools.warning;

public class UserEventManager implements UserEvent.Listener {

  /**
   * Constructor
   * 
   * @param defaultOper
   *          the default MouseOper, which will be active if no other is
   *          specified
   */
  public UserEventManager(UserOperation defaultOper) {
    if (defaultOper == null)
      throw new IllegalArgumentException();
    sDefaultOperation = defaultOper;
  }

  /**
   * Set enabled state of manager. It is constructed in DISABLED state; only
   * when enabled will it process any user events
   * 
   * @param enabled
   */
  public void setEnabled(boolean enabled) {
    mEnabled = enabled;
  }

  /**
   * Get current operation
   */
  public UserOperation getOperation() {
    if (sCurrentOperation == null)
      sCurrentOperation = sDefaultOperation;
    return sCurrentOperation;
  }

  /**
   * Set current operation
   */
  public void setOperation(UserOperation oper) {
    if (oper == null) {
      oper = sDefaultOperation;
    }
    if (sCurrentOperation != oper) {
      if (sCurrentOperation != null)
        sCurrentOperation.stop();
      sCurrentOperation = oper;
      if (sCurrentOperation != null)
        sCurrentOperation.start();
    }
  }

  public void clearOperation() {
    setOperation(null);
  }

  @Override
  public void processUserEvent(UserEvent event) {
    if (!mEnabled) {
      warning("UserEventManager isn't enabled, ignoring " + event.getCode());
      return;
    }
    mLastEventHandled = event;

    // Pass this event to the current operation
    getOperation().processUserEvent(event);

    // If an additional listener has been specified,
    // pass it along
    if (mListener != null) {
      mListener.processUserEvent(event);
    }
  }

  /**
   * Specify an optional listener, which will be passed the event after it's
   * been handled by the current operation
   */
  public void setListener(UserEvent.Listener listener) {
    mListener = listener;
  }

  public UserEvent getLastEventHandled() {
    return mLastEventHandled;
  }

  private boolean mEnabled;
  private UserEvent.Listener mListener;
  private UserOperation sDefaultOperation;
  private UserOperation sCurrentOperation;
  private UserEvent mLastEventHandled;
}
