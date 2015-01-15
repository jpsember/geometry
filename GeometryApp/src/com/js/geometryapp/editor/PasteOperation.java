package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import com.js.geometry.Point;

public class PasteOperation {

  public PasteOperation(Editor editor) {
    mEditor = editor;
  }

  public void attempt() {
    if (!isValid())
      return;

    CommandForGeneralChanges command = new CommandForGeneralChanges(mEditor,
        null, "Paste");
    EdObjectArray clipboard = command.getOriginalState().getClipboard();
    EditorState state = mEditor.getCurrentState();
    EdObjectArray objects = state.getObjects();
    if (!mEditor.verifyObjectsAllowed(objects.size() + clipboard.size()))
      return;
    SlotList newSelected = new SlotList();

    mEditor.setDupAffectsClipboard(true);
    mEditor.adjustDupAccumulatorForPendingOperation(clipboard);

    Point offset = state.getDupAccumulator();

    for (EdObject obj : clipboard) {
      newSelected.add(objects.size());
      EdObject copy = mutableCopyOf(obj);
      copy.moveBy(obj, offset);
      objects.add(copy);
    }
    objects.setSelected(newSelected);

    state.setClipboard(objects.getSelectedObjects());
    command.finish();
  }

  public boolean isValid() {
    return !mEditor.getCurrentState().getClipboard().isEmpty();
  }

  private Editor mEditor;
}
