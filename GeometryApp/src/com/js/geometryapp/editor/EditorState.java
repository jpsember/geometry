package com.js.geometryapp.editor;

import com.js.geometry.Point;
import static com.js.basic.Tools.*;
import com.js.basic.Freezable;

/**
 * Encapsulates the state of an editor, including all entities that are mutable
 * and need to be saved and restored to support undo/redo operations
 */
public class EditorState extends Freezable.Mutable {

  @Override
  public Freezable getMutableCopy() {
    EditorState copy = new EditorState(mutableCopyOf(mObjects), mClipboard,
        mDupAccumulator);
    return copy;
  }

  @Override
  public void freeze() {
    // We must override freeze() since we have components that need freezing as
    // well
    mObjects.freeze();
    super.freeze();
  }

  public EditorState() {
    this(null, null, null);
  }

  public EditorState(EdObjectArray objects, EdObjectArray clipboard,
      Point dupAccum) {
    if (objects == null)
      objects = new EdObjectArray();
    mObjects = mutable(objects);
    if (clipboard == null)
      clipboard = new EdObjectArray();
    mClipboard = frozen(clipboard);
    setDupAccumulatorAux(dupAccum);
  }

  public EdObjectArray getObjects() {
    return mObjects;
  }

  public EdObjectArray getClipboard() {
    return mClipboard;
  }

  private void setDupAccumulatorAux(Point dupAccum) {
    if (dupAccum == null)
      dupAccum = Point.ZERO;
    mDupAccumulator = new Point(dupAccum);
  }

  /**
   * Convenience method to get list of selected items from objects
   */
  public SlotList getSelectedSlots() {
    return mObjects.getSelectedSlots();
  }

  public Point getDupAccumulator() {
    return mDupAccumulator;
  }

  public void setDupAccumulator(Point accumulator) {
    mutate();
    setDupAccumulatorAux(accumulator);
  }

  public void setObjects(EdObjectArray objects) {
    mutate();
    if (objects == null)
      objects = new EdObjectArray();
    if (objects.isFrozen())
      throw new IllegalArgumentException();
    mObjects = objects;
  }

  public void setClipboard(EdObjectArray clipboard) {
    mutate();
    if (clipboard == null)
      clipboard = new EdObjectArray();
    mClipboard = frozen(clipboard);
  }

  private EdObjectArray mObjects;
  private EdObjectArray mClipboard;
  private Point mDupAccumulator;

}
