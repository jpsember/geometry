package com.js.geometryapp.editor;

import com.js.geometry.Point;
import static com.js.basic.Tools.*;

/**
 * Encapsulates the state of an editor, including all entities that are mutable
 * and need to be saved and restored to support undo/redo operations
 */
public class EditorState {

  public EditorState(Editor e) {
    mObjects = frozen(e.objects());
    mSelectedSlots = mObjects.getSelectedSlots();
    mClipboard = e.getClipboard();
    mDupAccumulator = e.getDupAccumulator();
  }

  public EdObjectArray getObjects() {
    return mObjects;
  }

  public EdObjectArray getClipboard() {
    return mClipboard;
  }

  public SlotList getSelectedSlots() {
    return mSelectedSlots;
  }

  public Point getDupAccumulator() {
    return mDupAccumulator;
  }

  private EdObjectArray mObjects;
  private SlotList mSelectedSlots;
  private EdObjectArray mClipboard;
  private Point mDupAccumulator;
}
