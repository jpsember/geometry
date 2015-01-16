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
    mDefaultOperation = defaultOper;
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
    if (mCurrentOperation == null)
      mCurrentOperation = mDefaultOperation;
    return mCurrentOperation;
  }

  /**
   * Set current operation. Stops existing, if one exists; then starts new. If
   * new is null, uses default operation.
   */
  public void setOperation(UserOperation oper) {
    if (mCurrentOperation != null) {
      mCurrentOperation.stop();
      mCurrentOperation = null;
    }

    if (oper == null) {
      oper = mDefaultOperation;
    }
    mCurrentOperation = oper;
    mCurrentOperation.start();
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

  /**
   * Perform an operation, if possible. Checks if operation is enableable; if
   * so, starts it and clears it
   */
  public void perform(UserOperation operation) {
    if (!operation.shouldBeEnabled())
      return;
    setOperation(operation);
    clearOperation();
  }

  private boolean mEnabled;
  private UserEvent.Listener mListener;
  private UserOperation mDefaultOperation;
  private UserOperation mCurrentOperation;
  private UserEvent mLastEventHandled;
}
