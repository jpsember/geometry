package com.js.geometryapp.editor;

import com.js.editor.Command;

public class CommandForGeneralChanges extends Command.Adapter {

  /**
   * Construct a command in preparation for changes. Saves current editor state.
   * Client should modify the state, and call finish() to mark the completion of
   * the command
   * 
   * @param editor
   * @param mergeKey
   * @param description
   */
  public CommandForGeneralChanges(Editor editor, String mergeKey,
      String description) {
    mEditor = editor;
    setOriginalState(editor.getStateSnapshot());
    mMergeKey = mergeKey;
    setDescription(description);
  }

  public void finish() {
    if (finished())
      throw new IllegalStateException();
    mEditor.disposeOfStateSnapshot();
    mNewState = mEditor.getStateSnapshot();
    // Push command onto editor stack
    mEditor.pushCommand(this);
  }

  public EditorState getOriginalState() {
    return mOriginalState;
  }

  private boolean finished() {
    return mNewState != null;
  }

  private CommandForGeneralChanges(Editor editor, EditorState originalState,
      EditorState newState, String mergeKey, String description) {
    mEditor = editor;
    setOriginalState(originalState);
    mNewState = newState;
    mMergeKey = mergeKey;
    setDescription(description);
  }

  @Override
  public Command getReverse() {
    if (mReverse == null) {
      mReverse = new CommandForGeneralChanges(mEditor, mNewState,
          mOriginalState, null, null);
    }
    return mReverse;
  }

  @Override
  public void perform() {
    mEditor.setState(mNewState);
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

      String mergedDescription = this.getDescription();
      if (mergedDescription == null)
        mergedDescription = follower.getDescription();

      // Merging is possible, so construct merged command
      merged = new CommandForGeneralChanges(mEditor, mOriginalState,
          f.mNewState, mMergeKey, mergedDescription);

    } while (false);
    return merged;
  }

  @Override
  public String toString() {
    if (mCommandDescription != null)
      return mCommandDescription;
    return "Last Command";
  }

  private void setOriginalState(EditorState s) {
    if (s.isMutable())
      throw new IllegalArgumentException();
    mOriginalState = s;
  }

  private Editor mEditor;
  private String mCommandDescription;
  private EditorState mOriginalState;
  private EditorState mNewState;
  private String mMergeKey;
  private Command mReverse;
}
