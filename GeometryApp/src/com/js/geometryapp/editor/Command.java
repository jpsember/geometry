package com.js.geometryapp.editor;

import java.util.List;
import static com.js.basic.Tools.*;

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

	/**
	 * Construct a Command for objects that have been added
	 * 
	 * @param editorObjects
	 *            the editor's objects
	 * @param slots
	 *            slots of objects that were added
	 */
	public static Command constructForAddedObjects(EdObjectArray editorObjects,
			List<Integer> slots) {
		return new CommandForAddedObjects(editorObjects.get(slots), slots);
	}

	/**
	 * Construct a Command for objects that have been removed
	 * 
	 * @param removedObjects
	 *            objects that were removed
	 * @param slots
	 *            their slots
	 */
	public static Command constructForRemovedObjects(
			EdObjectArray removedObjects, List<Integer> slots) {
		return new CommandForRemovedObjects(removedObjects, slots);
	}

	private static class CommandForModifiedObjects extends Command {

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Command: modified objects ");
			sb.append(d(mSlots));
			return sb.toString();
		}

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

	private static class CommandForAddedObjects extends Command {
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Command: added objects ");
			sb.append(d(mSlots));
			return sb.toString();
		}

		public CommandForAddedObjects(EdObjectArray addedObjects,
				List<Integer> slots) {
			mNew = addedObjects;
			mSlots = slots;
		}

		@Override
		public Command getReverse() {
			if (mReverse == null) {
				mReverse = constructForRemovedObjects(mNew, mSlots);
			}
			return mReverse;
		}

		@Override
		public void perform(Editor editor) {
			pr("replacing objects, slots " + d(mSlots) + " mNew " + d(mNew));
			editor.objects().replace(mSlots, mNew);
			editor.objects().select(mSlots);
		}

		@Override
		public boolean valid() {
			return true;
		}

		private List<Integer> mSlots;
		private EdObjectArray mNew;
		private Command mReverse;
	}

	private static class CommandForRemovedObjects extends Command {
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Command: removed objects ");
			sb.append(d(mSlots));
			return sb.toString();
		}

		public CommandForRemovedObjects(EdObjectArray removedObjects,
				List<Integer> slots) {
			mRemoved = removedObjects;
			mSlots = slots;
		}

		@Override
		public Command getReverse() {
			if (mReverse == null) {
				mReverse = new CommandForAddedObjects(mRemoved, mSlots);
			}
			return mReverse;
		}

		@Override
		public void perform(Editor editor) {
			editor.objects().remove(mSlots);
			editor.objects().unselectAll();
		}

		@Override
		public boolean valid() {
			return true;
		}

		private List<Integer> mSlots;
		private EdObjectArray mRemoved;
		private Command mReverse;
	}

}
