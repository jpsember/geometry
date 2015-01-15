package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import com.js.geometry.Point;

public class DupOperation {

  public DupOperation(Editor editor) {
    mEditor = editor;
  }

  public void attempt() {
    if (!isValid())
      return;
    CommandForGeneralChanges command = new CommandForGeneralChanges(mEditor,
        null, "Duplicate");
    EditorState state = command.getOriginalState();
    EdObjectArray objects = state.getObjects();
    if (state.getSelectedSlots().isEmpty())
      return;
    if (!mEditor.verifyObjectsAllowed(objects.size()
        + state.getSelectedSlots().size()))
      return;

    mEditor.adjustDupAccumulatorForPendingOperation(objects
.getSelectedObjects(), false);
    SlotList newSelected = new SlotList();

    Point offset = state.getDupAccumulator();

    for (int slot : state.getSelectedSlots()) {
      EdObject obj = objects.get(slot);
      EdObject copy = mutableCopyOf(obj);
      copy.moveBy(obj, offset);
      newSelected.add(objects.add(copy));
    }
    objects.setSelected(newSelected);
    command.finish();
  }

  public boolean isValid() {
    return !mEditor.getCurrentState().getSelectedSlots().isEmpty();
  }

  private Editor mEditor;
}
