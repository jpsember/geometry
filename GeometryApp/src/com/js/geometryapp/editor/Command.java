package com.js.geometryapp.editor;

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
	 * @param originalState
	 *            editor state before command performed
	 * @param newState
	 *            editor state after command performed
	 * @param mergeKey
	 *            if not null, a string that identifies whether this command can
	 *            be merged with its neighbors; if the keys match, and their
	 *            slots match, then merging will be performed
	 */
	public static Command constructForGeneralChanges(EditorState originalState,
			EditorState newState, String mergeKey) {
		return new CommandForGeneralChanges(originalState, newState, mergeKey);
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
			StringBuilder sb = new StringBuilder(
					"Command: general changes;\n orig state: ");
			sb.append(mOriginalState);
			sb.append("\n  new state: " + mNewState);
			return sb.toString();
		}

		public CommandForGeneralChanges(EditorState originalState,
				EditorState newState, String mergeKey) {
			mOriginalState = originalState;
			mNewState = newState;
			mMergeKey = mergeKey;
		}

		@Override
		public Command getReverse() {
			if (mReverse == null) {
				mReverse = new CommandForGeneralChanges(mNewState,
						mOriginalState, null);
			}
			return mReverse;
		}

		@Override
		public void perform(Editor editor) {
			editor.setState(mNewState);
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
				if (!mNewState.getSelectedSlots().equals(
						f.mOriginalState.getSelectedSlots()))
					break;

				// Merging is possible, so construct merged command
				merged = new CommandForGeneralChanges(mOriginalState,
						f.mNewState, mMergeKey);

			} while (false);
			return merged;
		}

		private EditorState mOriginalState;
		private EditorState mNewState;
		private String mMergeKey;
		private Command mReverse;
	}

}
