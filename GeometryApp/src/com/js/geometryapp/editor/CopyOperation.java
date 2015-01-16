package com.js.geometryapp.editor;

import com.js.editor.UserEvent;
import com.js.editor.UserOperation;

public class CopyOperation extends UserOperation {

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

  @Override
  public void processUserEvent(UserEvent event) {
    throw new UnsupportedOperationException();
  }

  private Editor mEditor;
}
