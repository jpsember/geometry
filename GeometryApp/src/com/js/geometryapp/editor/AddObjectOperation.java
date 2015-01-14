package com.js.geometryapp.editor;

import android.graphics.Color;

import com.js.editor.Command;
import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;
import com.js.geometry.Rect;

/**
 * Operation for adding new objects
 */
public class AddObjectOperation extends UserOperation {

  public AddObjectOperation(Editor editor, AlgorithmStepper stepper,
      EdObjectFactory factory) {
    mEditor = editor;
    mStepper = stepper;
    mObjectFactory = factory;
  }

  @Override
  public void processUserEvent(UserEvent event) {
    mEvent = event;

    switch (event.getCode()) {

    case UserEvent.CODE_DOWN:
      mInitialEvent = mEvent;
      addNewObject();
      break;

    case UserEvent.CODE_DRAG:
      mDragCorner = event.getWorldLocation();
      break;

    case UserEvent.CODE_UP:
      Rect dragRect = getDragRect();
      if (dragRect != null) {
        SlotList selectedList = new SlotList();
        for (int slot = 0; slot < mEditor.objects().size(); slot++) {
          EdObject edObject = mEditor.objects().get(slot);
          if (dragRect.contains(edObject.getBounds(mEditor.objects()
              .isSlotSelected(slot))))
            selectedList.add(slot);
        }
        mEditor.objects().setSelected(selectedList);
      }
      mEvent.clearOperation();
      break;
    }
  }

  @Override
  public void paint() {
    Rect r = getDragRect();
    if (r == null)
      return;
    AlgorithmStepper s = mStepper;
    s.setColor(Color.argb(0x80, 0xff, 0x40, 0x40));
    EditorTools.plotRect(s, r);
  }

  /**
   * Get the rectangle for the current drag selection operation, or null if none
   * is active (or rectangle is not available)
   * 
   * @return Rect, or null
   */
  private Rect getDragRect() {
    if (mDragCorner != null)
      return new Rect(mInitialEvent.getWorldLocation(), mDragCorner);
    return null;
  }

  private void addNewObject() {
    EdObjectArray objects = mEditor.objects();
    EditorState originalState = new EditorState(mEditor);
    EdObject newObject = mObjectFactory.construct(mEvent.getWorldLocation());
    newObject.setEditor(mEditor);
    int slot = objects.add(newObject);
    objects.setEditableSlot(slot);

    Command c = new CommandForGeneralChanges(mEditor, originalState, null,
        mObjectFactory.getTag(), null);
    mEditor.pushCommand(c);
    UserOperation oper = newObject.buildEditOperation(slot, mInitialEvent);
    mEvent.setOperation(oper);
    // The 'EdPoint' operation clears itself immediately for some reason
    if (mEvent.getOperation() == oper)
      oper.processUserEvent(mInitialEvent);

  }

  private AlgorithmStepper mStepper;
  private UserEvent mInitialEvent;
  private UserEvent mEvent;
  private Point mDragCorner;
  private Editor mEditor;
  private EdObjectFactory mObjectFactory;
}
