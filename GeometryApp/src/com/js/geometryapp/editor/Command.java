package com.js.geometryapp.editor;

/**
 * Encapsulates an edit operation, to allow for undo/redo functionality
 */
public interface Command {

	/**
	 * Get a command that will undo this one
	 */
	public Command getReverse();

	/**
	 * Perform this command
	 */
	public void perform();

	/**
	 * Determine if operation is valid in current context; essentially whether a
	 * menu item should be enabled or not
	 */
	public boolean valid();
}
