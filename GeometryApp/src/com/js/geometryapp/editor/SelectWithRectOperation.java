package com.js.geometryapp.editor;

import java.util.List;

import android.graphics.Color;

import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;
import com.js.geometry.Rect;

/**
 * Operation for selecting objects by dragging a rectangle
 */
public class SelectWithRectOperation extends UserOperation {

  public SelectWithRectOperation(Editor editor, AlgorithmStepper stepper) {
    mEditor = editor;
    mStepper = stepper;
  }

  @Override
  public void processUserEvent(UserEvent event) {
    event.printProcessingMessage("SelectWithRectOperation");

    mEvent = event;

    switch (event.getCode()) {

    case UserEvent.CODE_DOWN:
      mInitialEvent = mEvent;
      break;

    case UserEvent.CODE_DRAG:
      mDragCorner = event.getWorldLocation();
      break;

    case UserEvent.CODE_UP:
      Rect dragRect = getDragRect();
      if (dragRect != null) {
        List<Integer> selectedList = SlotList.build();
        for (int slot = 0; slot < mEditor.objects().size(); slot++) {
          EdObject edObject = mEditor.objects().get(slot);
          if (dragRect.contains(edObject.getBounds()))
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

  private AlgorithmStepper mStepper;
  private UserEvent mInitialEvent;
  private UserEvent mEvent;
  private Point mDragCorner;
  private Editor mEditor;
}
