package com.js.geometryapp.editor;

import java.util.List;

/**
 * Encapsulates an edit operation, to allow for undo/redo functionality
 */
public abstract class Command {

	/**
	 * Get a command that will undo this one
	 */
	public abstract Command getReverse();

	/**
	 * Perform this command
	 */
	public abstract void perform(Editor editor);

	/**
	 * Determine if operation is valid in current context; essentially whether a
	 * menu item should be enabled or not
	 */
	public boolean valid() {
		return true;
	}

	/**
	 * Construct a Command that generically saves and restores a subset of
	 * objects that have been edited
	 * 
	 * @param editorObjects
	 *            the editor's objects
	 * @param originalObjects
	 *            the subset of edited objects, before they were edited; must
	 *            include the slots
	 */
	public static Command constructForEditedObjects(
			EdObjectArray editorObjects, EdObjectArray originalObjects) {
		if (originalObjects.getSlots() == null)
			throw new IllegalArgumentException("no slots available");
		return new CommandForModifiedObjects(editorObjects,
				originalObjects.getSlots(), originalObjects);
	}

	private static class CommandForModifiedObjects extends Command {
		public CommandForModifiedObjects(EdObjectArray editorObjects,
				List<Integer> slots, EdObjectArray originalObjects) {
			mNew = editorObjects.get(slots);
			mSlots = slots;
			mOriginals = originalObjects;
		}

		private CommandForModifiedObjects() {
		}

		@Override
		public Command getReverse() {
			if (mReverse == null) {
				CommandForModifiedObjects c = new CommandForModifiedObjects();
				c.mSlots = this.mSlots;
				c.mOriginals = this.mNew;
				c.mNew = this.mOriginals;
				mReverse = c;
			}
			return mReverse;
		}

		@Override
		public void perform(Editor editor) {
			editor.objects().replace(mSlots, mNew);
			editor.objects().select(mSlots);
		}

		@Override
		public boolean valid() {
			return true;
		}

		private List<Integer> mSlots;
		private EdObjectArray mOriginals;
		private EdObjectArray mNew;
		private Command mReverse;
	}
}
