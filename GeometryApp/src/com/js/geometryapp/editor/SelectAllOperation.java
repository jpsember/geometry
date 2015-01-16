package com.js.geometryapp.editor;

import com.js.editor.UserOperation;

public class SelectAllOperation extends UserOperation.InstantOperation {

  public SelectAllOperation(Editor editor) {
    mEditor = editor;
  }

  @Override
  public void start() {
    if (!shouldBeEnabled())
      return;
    CommandForGeneralChanges command = new CommandForGeneralChanges(mEditor,
        null, "Select All");
    mEditor.objects().selectAll();
    command.finish();
  }

  @Override
  public boolean shouldBeEnabled() {
    EditorState s = mEditor.getCurrentState();
    return s.getSelectedSlots().size() < s.getObjects().size();
  }

  private Editor mEditor;

}
