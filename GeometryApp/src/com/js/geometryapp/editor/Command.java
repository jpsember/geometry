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
	 * Construct a Command for arbitrary objects having changed, including
	 * selected states, and clipboard
	 * 
	 * @param originalObjects
	 *            array containing all of the original objects
	 * @param originalSelectedSlots
	 *            slots of the items selected originally
	 * @param originalClipboard
	 *            original clipboard contents, or null if this command doesn't
	 *            modify the clipboard
	 * @param newObjects
	 *            array containing all of the objects, including any
	 *            modifications
	 * @param newSelectedSlots
	 *            slots of the selected items after the command, or null if same
	 *            as originalSelectedSlots
	 * @param newClipboard
	 *            new clipboard contents, or null if this command doesn't modify
	 *            the clipboard
	 * @param mergeKey
	 *            if not null, a string that identifies whether this command can
	 *            be merged with its neighbors; if the keys match, and their
	 *            slots match, then merging will be performed
	 */
	public static Command constructForGeneralChanges(
			EdObjectArray originalObjects, List<Integer> originalSelectedSlots,
			EdObjectArray originalClipboard, EdObjectArray newObjects,
			List<Integer> newSelectedSlots, EdObjectArray newClipboard,
			String mergeKey) {
		return new CommandForGeneralChanges(originalObjects,
				originalSelectedSlots, originalClipboard, newObjects,
				newSelectedSlots, newClipboard, mergeKey);
	}

	private static class CommandForGeneralChanges extends Command {

		// TODO: we could do a more sophisticated analysis of exactly which
		// objects are being changed, and 'difference encode' the various
		// arrays;
		// this would take up less memory (though it doesn't store
		// copies of objects, rather copies of their pointers). The savings
		// come from not having to store O(n*u) pointers for object array |n|
		// and undo stack |u|. Rather, something like O(k * u) where k is a
		// small constant k << n.

		@Override
		public String toString() {
			if (!DEBUG_ONLY_FEATURES)
				return null;
			StringBuilder sb = new StringBuilder("Command: general changes ");
			sb.append(d(mOriginalObjects));
			sb.append(d(mOriginalSelectedSlots));
			sb.append("clipboard: " + d(mOriginalClipboard));
			return sb.toString();
		}

		public CommandForGeneralChanges(EdObjectArray originalObjects,
				List<Integer> originalSelectedSlots,
				EdObjectArray originalClipboard, EdObjectArray newObjects,
				List<Integer> newSelectedSlots, EdObjectArray newClipboard,
				String mergeKey) {
			mOriginalObjects = originalObjects.getFrozen();
			mOriginalSelectedSlots = originalSelectedSlots;
			mOriginalClipboard = originalClipboard;
			mNewObjects = newObjects.getFrozen();
			mNewSelectedSlots = newSelectedSlots;
			if (newSelectedSlots == null)
				mNewSelectedSlots = mOriginalSelectedSlots;
			mNewClipboard = newClipboard;
			mMergeKey = mergeKey;
		}

		@Override
		public Command getReverse() {
			if (mReverse == null) {
				mReverse = new CommandForGeneralChanges(mNewObjects,
						mNewSelectedSlots, mNewClipboard, mOriginalObjects,
						mOriginalSelectedSlots, mOriginalClipboard, null);
			}
			return mReverse;
		}

		@Override
		public void perform(Editor editor) {
			editor.setObjects(mNewObjects.getMutableCopy());
			if (mNewClipboard != null)
				editor.setClipboard(mNewClipboard);
			editor.objects().selectOnly(mNewSelectedSlots);
		}

		@Override
		public Command attemptMergeWith(Command follower) {
			CommandForGeneralChanges merged = null;
			do {
				if (mMergeKey == null)
					break;

				if (!(follower instanceof CommandForGeneralChanges))
					break;
				CommandForGeneralChanges f = (CommandForGeneralChanges) follower;

				if (!mMergeKey.equals(f.mMergeKey))
					break;

				// The selection after the first command was executed must equal
				// the selection before the second command was.
				if (!mNewSelectedSlots.equals(f.mOriginalSelectedSlots))
					break;

				// Merging is possible, so construct merged command
				merged = new CommandForGeneralChanges(mOriginalObjects,
						mOriginalSelectedSlots, mOriginalClipboard,
						f.mNewObjects, f.mNewSelectedSlots, f.mNewClipboard,
						mMergeKey);

			} while (false);
			return merged;
		}

		private EdObjectArray mOriginalObjects, mNewObjects,
				mOriginalClipboard, mNewClipboard;
		private List<Integer> mOriginalSelectedSlots, mNewSelectedSlots;
		private String mMergeKey;
		private Command mReverse;
	}

}
