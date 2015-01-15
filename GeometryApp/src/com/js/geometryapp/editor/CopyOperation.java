package com.js.geometryapp.editor;

public class CopyOperation {

  public CopyOperation(Editor editor) {
    mEditor = editor;
  }

  public void attempt() {
    if (!isValid())
      return;
    CommandForGeneralChanges c = new CommandForGeneralChanges(mEditor, null,
        "Copy");
    EditorState s = mEditor.getCurrentState();
    s.setClipboard(s.getObjects().getSelectedObjects());
    s.resetDupAccumulator();
    c.finish();
  }

  public boolean isValid() {
    return !mEditor.getCurrentState().getSelectedSlots().isEmpty();
  }

  private Editor mEditor;
}
