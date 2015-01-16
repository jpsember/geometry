package com.js.geometryapp.editor;

import com.js.editor.UserOperation;

public class CutOperation extends UserOperation.InstantOperation {

  public CutOperation(Editor editor) {
    mEditor = editor;
  }

  @Override
  public boolean shouldBeEnabled() {
    return !mEditor.getCurrentState().getSelectedSlots().isEmpty();
  }

  @Override
  public void start() {
    CommandForGeneralChanges c = new CommandForGeneralChanges(mEditor, null,
        "Cut");
    EdObjectArray objects = c.getOriginalState().getObjects();
    SlotList allSlots = SlotList.buildComplete(objects.size());
    SlotList selectedSlots = objects.getSelectedSlots();
    SlotList newSlots = allSlots.minus(selectedSlots);

    EdObjectArray newObjects = objects.getSubset(newSlots);
    EdObjectArray newClipboard = objects.getSubset(selectedSlots);

    EditorState state = mEditor.getCurrentState();
    state.setObjects(newObjects);
    state.setClipboard(newClipboard);
    state.resetDupAccumulator();

    c.finish();
  }

  private Editor mEditor;

}
