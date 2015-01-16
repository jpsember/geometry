package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.Point;

public class PasteOperation extends UserOperation {

  public PasteOperation(Editor editor) {
    mEditor = editor;
  }

  @Override
  public void start() {

    CommandForGeneralChanges command = new CommandForGeneralChanges(mEditor,
        null, "Paste");
    EdObjectArray clipboard = command.getOriginalState().getClipboard();
    EditorState state = mEditor.getCurrentState();
    EdObjectArray objects = state.getObjects();
    if (!mEditor.verifyObjectsAllowed(objects.size() + clipboard.size()))
      return;
    SlotList newSelected = new SlotList();

    mEditor.adjustDupAccumulatorForPendingOperation(clipboard, true);

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

  @Override
  public boolean shouldBeEnabled() {
    return !mEditor.getCurrentState().getClipboard().isEmpty();
  }

  @Override
  public void processUserEvent(UserEvent event) {
    throw new UnsupportedOperationException();
  }

  private Editor mEditor;

}
