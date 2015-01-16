package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import com.js.editor.UserOperation;
import com.js.geometry.Point;

public class DupOperation extends UserOperation.InstantOperation {

  public DupOperation(Editor editor) {
    mEditor = editor;
  }

  @Override
  public void start() {
    if (!shouldBeEnabled())
      return;
    CommandForGeneralChanges command = new CommandForGeneralChanges(mEditor,
        null, "Duplicate");

    EditorState state = command.getOriginalState();
    EdObjectArray origObjects = state.getObjects();
    if (state.getSelectedSlots().isEmpty())
      return;
    if (!mEditor.verifyObjectsAllowed(state.getSelectedSlots().size()))
      return;

    mEditor.adjustDupAccumulatorForPendingOperation(
        origObjects.getSelectedObjects(), false);
    SlotList newSelected = new SlotList();

    Point offset = state.getDupAccumulator();

    EdObjectArray currObjects = mEditor.getCurrentState().getObjects();

    for (int slot : state.getSelectedSlots()) {
      EdObject obj = origObjects.get(slot);
      EdObject copy = mutableCopyOf(obj);
      copy.moveBy(obj, offset);
      newSelected.add(currObjects.add(copy));
    }
    currObjects.setSelected(newSelected);
    command.finish();
  }

  @Override
  public boolean shouldBeEnabled() {
    return !mEditor.getCurrentState().getSelectedSlots().isEmpty();
  }

  private Editor mEditor;

}
