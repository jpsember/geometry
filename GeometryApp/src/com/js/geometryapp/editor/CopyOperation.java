package com.js.geometryapp.editor;

import com.js.editor.UserOperation;

public class CopyOperation extends UserOperation.InstantOperation {

  public CopyOperation(Editor editor) {
    mEditor = editor;
  }

  @Override
  public void start() {
    CommandForGeneralChanges c = new CommandForGeneralChanges(mEditor, null,
        "Copy");
    EditorState s = mEditor.getCurrentState();
    s.setClipboard(s.getObjects().getSelectedObjects());
    s.resetDupAccumulator();
    c.finish();
  }

  @Override
  public boolean shouldBeEnabled() {
    return !mEditor.getCurrentState().getSelectedSlots().isEmpty();
  }

  private Editor mEditor;
}
