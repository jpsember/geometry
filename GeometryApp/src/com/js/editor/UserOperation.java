package com.js.editor;

/**
 * Class representing a user operation, usually involving the mouse (or touch
 * device)
 */
public abstract class UserOperation implements UserEvent.Listener, Enableable {

  @Override
  public abstract void processUserEvent(UserEvent event);

  /**
   * Enableable interface; by default, always returns true
   */
  @Override
  public boolean shouldBeEnabled() {
    return true;
  }

  /**
   * Determine if an object can be editable during this operation. Default is
   * true; for some operations, e.g. rotation and scaling, this will be false
   */
  public boolean allowEditableObject() {
    return true;
  }

  /**
   * Display any highlighting associated with this operation
   */
  public void paint() {
  }

  /**
   * Called when operation is starting
   */
  public void start() {
  }

  /**
   * Called when operation is stopping
   */
  public void stop() {
  }

  /**
   * Concrete subclass of UserOperation suitable for operations that don't
   * persist over a series of user interactions, such as cut/copy/paste
   */
  public static class InstantOperation extends UserOperation {
    @Override
    public final void processUserEvent(UserEvent event) {
      throw new UnsupportedOperationException();
    }

  }
}
