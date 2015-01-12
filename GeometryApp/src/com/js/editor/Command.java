package com.js.editor;

/**
 * Encapsulates a command that modifies the state of what is being edited.
 * 
 * Supports undo/redo functionality.
 */
public interface Command {

  /**
   * Get a command that will undo this one
   * 
   * Note: a command that is already an 'undo' of another one doesn't itself
   * need an undo, since the original command serves that purpose
   */
  public Command getReverse();

  /**
   * Perform this command
   */
  public void perform();

  /**
   * Merge this command with another that follows it, if possible. For example,
   * if user moves the same selected objects with several consecutive drag
   * operations, it makes sense that they all get merged into a single 'move'
   * command
   * 
   * @param follower
   *          command that follows this one
   * @return merged command, or null if no merge was possible
   */
  public Command attemptMergeWith(Command follower);

  /**
   * Determine if the command is valid; e.g., whether it can be performed at the
   * present time
   */
  public boolean valid();

  /**
   * Get description of command; this could show up, e.g., in an 'undo' or
   * 'redo' menu item
   */
  public String getDescription();

  /**
   * Set description of command
   */
  public void setDescription(String description);

  /**
   * Abstract class that implements the Command interface
   */
  public abstract class Adapter implements Command {

    @Override
    public Command getReverse() {
      throw new UnsupportedOperationException();
    }

    @Override
    public abstract void perform();

    /**
     * Default implementation: no merging possible
     */
    @Override
    public Command attemptMergeWith(Command follower) {
      return null;
    }

    /**
     * Default implementation: always enabled
     */
    @Override
    public boolean valid() {
      return true;
    }

    @Override
    public String getDescription() {
      return mDescription;
    }

    @Override
    public void setDescription(String description) {
      mDescription = description;
    }

    private String mDescription;
  }
}
