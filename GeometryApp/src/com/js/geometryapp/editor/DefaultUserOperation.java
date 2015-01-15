package com.js.geometryapp.editor;

import com.js.editor.*;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import static com.js.basic.Tools.*;

public class DefaultUserOperation extends UserOperation {

  public DefaultUserOperation(Editor editor, AlgorithmStepper stepper) {
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
    // event.printProcessingMessage("DefaultEventListener");

    mEvent = event;
    if (event.getCode() != UserEvent.CODE_DOWN && !operActive())
      return;

    switch (event.getCode()) {

    case UserEvent.CODE_DOWN:
      mActive = true;
      mInitialEvent = event;
      mDragOperation = false;
      break;

    case UserEvent.CODE_DRAG:
      if (event.isMultipleTouch()) {
        event.clearOperation();
        return;
      }
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

  private SlotList getPickSet(Point location) {
    SlotList slots = new SlotList();
    EdObjectArray srcObjects = mEditor.objects();
    float pickRadius = mEditor.pickRadius();
    for (int slot = 0; slot < srcObjects.size(); slot++) {
      EdObject src = srcObjects.get(slot);
      float dist = src.distFrom(location);
      if (srcObjects.isSlotSelected(slot))
        pickRadius *= 2f;
      if (dist > pickRadius)
        continue;
      slots.add(slot);
    }
    return slots;
  }

  /**
   * Given an item list, get subsequence of those items that are selected
   * 
   * @param slotList
   *          list of item slots
   * @return subsequence of slotList that are selected
   */
  private SlotList getSelectedObjects(SlotList slotList) {
    return SlotList
        .intersection(mEditor.objects().getSelectedSlots(), slotList);
  }

  /**
   * User did a DOWN+UP without any dragging
   * 
   * @param location
   *          location where DOWN occurred
   */
  private void doClick(Point location) {

    // If multitouch, start 'add another object'
    if (mInitialEvent.isMultipleTouch()) {
      EdObjectFactory factory = mEditor.getLastEditableObjectType();
      if (factory == null)
        mEvent.clearOperation();
      else
        mEditor.doStartAddObjectOperation(factory);
      return;
    }

    // Construct pick set of selected objects. If empty, unselect
    // all objects; else cycle to next object and make it editable
    SlotList pickSet = getPickSet(location);
    if (pickSet.isEmpty()) {
      mEditor.objects().unselectAll();
      mEditor.resetDuplicationOffset();
      return;
    }

    // Find selected item with highest index
    int highestIndex = pickSet.size();
    for (int i = 0; i < pickSet.size(); i++) {
      if (mEditor.objects().isSlotSelected(pickSet.get(i)))
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

    if (modifyEditableObject())
      return;

    // get 'pick set' for touch location
    SlotList pickSet = getPickSet(location);
    // get subset of pick set that are currently selected
    SlotList hlPickSet = getSelectedObjects(pickSet);

    if (hlPickSet.isEmpty() && !pickSet.isEmpty()) {
      hlPickSet = new SlotList(pickSet.last());
      mEditor.objects().setSelected(hlPickSet);
      mEditor.resetDuplicationOffset();
      // fall through to next...
    }
    if (!hlPickSet.isEmpty()) {
      mCommand = new CommandForGeneralChanges(mEditor, "move", null);

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

  /**
   * Determine if there's an editable object which can construct an edit
   * operation for a particular location. If so, start the operation and return
   * true
   * 
   * @param event
   *          EditorEvent; if not (single) DOWN event, always returns false
   */
  private boolean modifyEditableObject() {
    UserEvent downEvent = mInitialEvent;

    int editableSlot = mEditor.getEditableSlot();
    if (editableSlot < 0)
      return false;

    EdObject obj = mEditor.objects().get(editableSlot);
    UserOperation operation = obj.buildEditOperation(editableSlot, downEvent);

    if (operation == null)
      return false;

    downEvent.setOperation(operation);
    operation.processUserEvent(downEvent);
    return true;
  }

  private void doContinueDrag(Point location) {
    mTranslate = MyMath.subtract(location, mInitialEvent.getWorldLocation());
    if (mTranslate.magnitude() == 0)
      return;

    for (int slot : mCommand.getOriginalState().getSelectedSlots()) {
      EdObject orig = mCommand.getOriginalState().getObjects().get(slot);
      EdObject obj = mutableCopyOf(orig);
      obj.moveBy(orig, mTranslate);
      mEditor.objects().set(slot, obj);
    }
  }

  private void doFinishDrag() {
    if (mCommand == null)
      return;

    if (mTranslate != null) {
      mEditor.updateDupAccumulatorForTranslation(mTranslate);
    }
    mCommand.finish();
  }

  private Editor mEditor;
  private boolean mActive;
  private UserEvent mInitialEvent;
  private boolean mDragOperation;
  private CommandForGeneralChanges mCommand;
  private Point mTranslate;
  private AlgorithmStepper mStepper;
  private UserEvent mEvent;

}
