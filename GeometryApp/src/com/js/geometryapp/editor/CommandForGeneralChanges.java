package com.js.geometryapp.editor;

import com.js.editor.Command;

public class CommandForGeneralChanges extends Command.Adapter {

  /**
   * Constructor
   * 
   * @param originalState
   * @param newState
   *          editor state after change; if null, constructs from current
   *          editor's state
   * @param mergeKey
   *          optional merge key
   */
  public CommandForGeneralChanges(Editor editor, EditorState originalState,
      EditorState newState, String mergeKey, String description) {
    mEditor = editor;
    if (newState == null)
      newState = new EditorState(editor);
    mOriginalState = originalState;
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

  private Editor mEditor;
  private String mCommandDescription;
  private EditorState mOriginalState;
  private EditorState mNewState;
  private String mMergeKey;
  private Command mReverse;
}
