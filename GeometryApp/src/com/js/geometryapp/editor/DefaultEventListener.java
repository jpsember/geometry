package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import java.util.List;

import com.js.editor.*;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.MyMath;
import com.js.geometry.Point;

public class DefaultEventListener extends UserOperation {

  public DefaultEventListener(Editor editor, AlgorithmStepper stepper) {
    mEditor = editor;
    mStepper = stepper;
  }

  @Override
  public void stop() {
    mActive = false;
  }

  /**
   * <pre>
   * 
   * Picking behaviour
   * 
   * The 'pick set' under a point p is the ordered sequence of all objects that contain p. These
   * objects may or may not be currently selected.  Some objects, such as segments, should have
   * a fuzzy definition of what it means to contain a point.
   * 
   * Repeated clicks at a point should cycle between the pick set under that point, by selecting 
   * each item in sequence.
   * 
   * Clicking should unselect all objects that are not in the current point's click set.
   * 
   * There should be a distinction between clicking (down+up) and dragging (down+drag+up).
   * 
   * A down event on an empty pick set, followed by a drag, should produce a rectangle used to
   * select contained objects.
   * 
   * </pre>
   */
  @Override
  public void processUserEvent(UserEvent event) {
    final boolean db = true && DEBUG_ONLY_FEATURES;
    if (db)
      event.printProcessingMessage("DefaultEventListener");

    mEvent = event;
    if (event.getCode() != UserEvent.CODE_DOWN && !operActive())
      return;

    switch (event.getCode()) {

    case UserEvent.CODE_DOWN:
      if (event.isMultipleTouch())
        break;
      mActive = true;
      mInitialEvent = event;
      mDragOperation = false;
      break;

    case UserEvent.CODE_DRAG:
      if (!mDragOperation) {
        mDragOperation = true;
        doStartDrag(mInitialEvent.getWorldLocation());
      }
      if (!operActive())
        return;
      doContinueDrag(event.getWorldLocation());
      break;

    case UserEvent.CODE_UP:
      if (!mDragOperation) {
        doClick(mInitialEvent.getWorldLocation());
      } else {
        doFinishDrag();
      }
      break;
    }
  }

  private boolean operActive() {
    return mActive;
  }

  private List<Integer> getPickSet(Point location) {
    List<Integer> slots = SlotList.build();
    EdObjectArray srcObjects = mEditor.objects();
    float pickRadius = mEditor.pickRadius();
    for (int slot = 0; slot < srcObjects.size(); slot++) {
      EdObject src = srcObjects.get(slot);
      float dist = src.distFrom(location);
      if (src.isSelected())
        pickRadius *= 2f;
      if (dist > pickRadius)
        continue;
      slots.add(slot);
    }
    return slots;
  }

  private EdObject editorObject(int slot) {
    return mEditor.objects().get(slot);
  }

  /**
   * Given an item list, get subsequence of those items that are selected
   * 
   * @param slotList
   *          list of item slots
   * @return subsequence of slotList that are selected
   */
  private List<Integer> getSelectedObjects(List<Integer> slotList) {
    List<Integer> selectedSlots = SlotList.build();
    for (int slot : slotList) {
      EdObject obj = editorObject(slot);
      if (obj.isSelected())
        selectedSlots.add(slot);
    }
    return selectedSlots;
  }

  /**
   * User did a DOWN+UP without any dragging
   * 
   * @param location
   *          location where DOWN occurred
   */
  private void doClick(Point location) {

    // Construct pick set of selected objects. If empty, unselect
    // all objects; else cycle to next object and make it editable
    List<Integer> pickSet = getPickSet(location);
    if (pickSet.isEmpty()) {
      mEditor.objects().unselectAll();
      mEditor.resetDuplicationOffset();
      return;
    }

    // Find selected item with highest index
    int highestIndex = pickSet.size();
    for (int i = 0; i < pickSet.size(); i++) {
      if (mEditor.objects().get(pickSet.get(i)).isSelected())
        highestIndex = i;
    }
    int nextSelectedIndex = MyMath.myMod(highestIndex - 1, pickSet.size());
    int slot = pickSet.get(nextSelectedIndex);
    mEditor.objects().setEditableSlot(slot);
    mEditor.objects().get(slot).selectedForEditing(location);
    mEditor.resetDuplicationOffset();
  }

  private void doStartDrag(Point location) {

    /**
     * <pre>
     * 
     * If initial press pickset contains any selected objects, move all 
     * selected objects;
     * 
     * else
     * 
     * If initial press pickset contains any objects, select and move topmost;
     * 
     * else
     * 
     * If 'add multiple' is selected, and previous add object type defined, 
     * start adding another;
     * 
     * else
     * 
     * Drag a selection rectangle and select the items contained within it
     * 
     * </pre>
     */

    // get 'pick set' for touch location
    List<Integer> pickSet = getPickSet(location);
    // get subset of pick set that are currently selected
    List<Integer> hlPickSet = getSelectedObjects(pickSet);

    if (hlPickSet.isEmpty() && !pickSet.isEmpty()) {
      hlPickSet = SlotList.build(last(pickSet));
      mEditor.objects().setSelected(hlPickSet);
      mEditor.resetDuplicationOffset();
      // fall through to next...
    }
    if (!hlPickSet.isEmpty()) {
      mOriginalState = new EditorState(mEditor);
      // Replace selected objects with copies in preparation for moving
      mEditor.objects().replaceSelectedObjectsWithCopies();
    } else {
      UserOperation oper = new SelectWithRectOperation(mEditor, mStepper);
      mEvent.setOperation(oper);
      // Send the down, drag events to the new operation.
      oper.processUserEvent(mInitialEvent);
      oper.processUserEvent(mEvent);
    }
  }

  private void doContinueDrag(Point location) {
    mTranslate = MyMath.subtract(location, mInitialEvent.getWorldLocation());
    if (mTranslate.magnitude() == 0)
      return;

    for (int slot : mOriginalState.getSelectedSlots()) {
      EdObject obj = mEditor.objects().get(slot);
      EdObject orig = mOriginalState.getObjects().get(slot);
      obj.moveBy(orig, mTranslate);
    }
  }

  private void doFinishDrag() {
    if (mOriginalState != null) {
      if (mTranslate != null) {
        mEditor.updateDupAccumulatorForTranslation(mTranslate);
      }
      Command cmd = new CommandForGeneralChanges(mEditor, mOriginalState,
          new EditorState(mEditor), "move", null);
      mEditor.pushCommand(cmd);
    }
  }

  private Editor mEditor;
  private boolean mActive;
  private UserEvent mInitialEvent;
  private boolean mDragOperation;
  private EditorState mOriginalState;
  private Point mTranslate;
  private AlgorithmStepper mStepper;
  private UserEvent mEvent;

}
