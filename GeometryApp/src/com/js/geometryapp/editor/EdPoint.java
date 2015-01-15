package com.js.geometryapp.editor;

import android.graphics.Color;

import com.js.basic.Freezable;
import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;
import static com.js.basic.Tools.*;

public class EdPoint extends EdObject {

  private EdPoint(EdPoint source) {
    super(source);
  }

  @Override
  public Freezable getMutableCopy() {
    return new EdPoint(this);
  }

  private Point location() {
    return getPoint(0);
  }

  @Override
  public boolean valid() {
    return nPoints() == 1;
  }

  @Override
  public float distFrom(Point pt) {
    return MyMath.distanceBetween(pt, location());
  }

  @Override
  public EdObjectFactory getFactory() {
    return FACTORY;
  }

  @Override
  public UserOperation buildEditOperation(int slot, UserEvent initialPress) {
    Point location = initialPress.getWorldLocation();
    int vertexIndex = closestVertex(location, editor().pickRadius());
    if (vertexIndex >= 0)
      return new EditorOperation(editor(), slot);
    return null;
  }

  @Override
  public void render(AlgorithmStepper s, boolean selected, boolean editable) {
    if (selected) {
      super.render(s, selected, editable);
    } else {
      s.setColor(Color.BLUE);
      s.render(getPoint(0));
    }
  }

  public static EdObjectFactory FACTORY = new EdObjectFactory("p") {
    @Override
    public EdObject construct(Point defaultLocation) {
      EdPoint pt = new EdPoint(null);
      if (defaultLocation != null)
        pt.addPoint(defaultLocation);
      return pt;
    }

    @Override
    public int getIconResource() {
      return R.raw.pointicon;
    }
  };

  private static class EditorOperation extends UserOperation {
    public EditorOperation(Editor editor, int slot) {
      mEditor = editor;
      mEditSlot = slot;
      mCommand = new CommandForGeneralChanges(mEditor, FACTORY.getTag(), null);
    }

    @Override
    public void processUserEvent(UserEvent event) {

      switch (event.getCode()) {

      case UserEvent.CODE_DRAG: {
        EdPoint point = mEditor.objects().get(mEditSlot);
        // Create a new copy of the object, with modified vertex
        EdPoint point2 = mutableCopyOf(point);
        point2.setPoint(0, event.getWorldLocation());
        mEditor.objects().set(mEditSlot, point2);
        mModified = true;
      }
        break;

      case UserEvent.CODE_UP:
        if (mModified)
          mCommand.finish();
        event.clearOperation();
        break;
      }
    }

    // Index of point being edited
    private int mEditSlot;
    private boolean mModified;
    private CommandForGeneralChanges mCommand;
    private Editor mEditor;
  }

}
