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

	public boolean isPairedWithNext() {
		return mPairedWithNext;
	}

	/**
	 * Set flag indicating this command and its follower should be considered an
	 * atomic unit (see issue #116)
	 */
	public void setPairedWithNext(boolean f) {
		mPairedWithNext = f;
	}

	/**
	 * Merge this command with another that follows it, if possible. This
	 * addresses issue #114: for instance, multiple 'move' commands involving
	 * the same set of objects should be merged into one
	 * 
	 * @param follower
	 *            command that follows this one
	 * @return merged command, or null if no merge was possible
	 */
	public Command attemptMergeWith(Command follower) {
		return null;
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
	 * @param mergeKey
	 *            if not null, a string that identifies whether this command can
	 *            be merged with its neighbors; if the keys match, and their
	 *            slots match, then merging will be performed
	 */
	public static Command constructForEditedObjects(
			EdObjectArray editorObjects, EdObjectArray originalObjects,
			String mergeKey) {
		if (originalObjects.getSlots() == null)
			throw new IllegalArgumentException("no slots available");
		return new CommandForModifiedObjects(editorObjects,
				originalObjects.getSlots(), originalObjects, mergeKey);
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
			if (!DEBUG_ONLY_FEATURES)
				return null;
			StringBuilder sb = new StringBuilder("Command: modified objects ");
			sb.append(d(mSlots));
			if (mMergeKey != null)
				sb.append(" mergeKey:" + mMergeKey);
			return sb.toString();
		}

		public CommandForModifiedObjects(EdObjectArray editorObjects,
				List<Integer> slots, EdObjectArray originalObjects,
				String mergeKey) {
			mNew = editorObjects.get(slots);
			mSlots = slots;
			mOriginals = originalObjects;
			mMergeKey = mergeKey;
		}

		private CommandForModifiedObjects() {
		}

		@Override
		public Command attemptMergeWith(Command follower) {
			CommandForModifiedObjects merged = null;
			do {
				if (mMergeKey == null)
					break;
				if (!(follower instanceof CommandForModifiedObjects))
					break;
				CommandForModifiedObjects f = (CommandForModifiedObjects) follower;
				if (!mMergeKey.equals(f.mMergeKey))
					break;
				if (!mSlots.equals(f.mSlots))
					break;

				// Merging is possible, so construct merged command
				merged = new CommandForModifiedObjects();
				merged.mOriginals = this.mOriginals;
				merged.mNew = f.mNew;
				merged.mSlots = this.mSlots;
				merged.mMergeKey = this.mMergeKey;
			} while (false);
			return merged;
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
		private String mMergeKey;
	}

	private static class CommandForAddedObjects extends Command {
		@Override
		public String toString() {
			if (!DEBUG_ONLY_FEATURES)
				return null;
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
			if (!DEBUG_ONLY_FEATURES)
				return null;
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

	private boolean mPairedWithNext;
}
