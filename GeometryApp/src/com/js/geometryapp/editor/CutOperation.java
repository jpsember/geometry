package com.js.geometryapp.editor;

/**
 * Note: this is not a UserOperation, since it requires no user interaction
 */
public class CutOperation {

  public static void attempt(Editor editor) {
    CommandForGeneralChanges c = new CommandForGeneralChanges(editor, null,
        "Cut");
    if (c.getOriginalState().getSelectedSlots().isEmpty())
      return;

    EdObjectArray objects = c.getOriginalState().getObjects();
    SlotList allSlots = SlotList.buildComplete(objects.size());
    SlotList selectedSlots = objects.getSelectedSlots();
    SlotList newSlots = allSlots.minus(selectedSlots);

    EdObjectArray newObjects = objects.getSubset(newSlots);
    EdObjectArray newClipboard = objects.getSubset(selectedSlots);

    EditorState state = editor.getCurrentState();
    state.setObjects(newObjects);
    state.setClipboard(newClipboard);
    state.setDupAccumulator(null);

    c.finish();
  }

}
