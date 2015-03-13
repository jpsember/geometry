package com.js.geometryapp.editor;

import android.graphics.Color;

import com.js.basic.Freezable;
import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.AlgorithmStepper;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.geometry.R;
import static com.js.basic.Tools.*;

public class EdSegment extends EdObject {

  private EdSegment(EdSegment source) {
    super(source);
    if (source == null)
      return;
  }

  @Override
  public Freezable getMutableCopy() {
    return new EdSegment(this);
  }

  @Override
  public boolean valid() {
    return nPoints() == 2;
  }

  @Override
  public void render(AlgorithmStepper s, boolean selected, boolean editable) {
    s.setColor(Color.BLUE);
    renderLine(s, getPoint(0), getPoint(1));
    super.render(s, selected, editable);
  }

  @Override
  public UserOperation buildEditOperation(int slot, UserEvent initialPress) {
    Point location = initialPress.getWorldLocation();
    int vertexIndex = closestVertex(location, editor().pickRadius());
    if (vertexIndex >= 0)
      return new EditorOperation(editor(), slot, vertexIndex);
    return null;
  }

  public float distFrom(Point pt) {
    Point p1 = getPoint(0);
    Point p2 = getPoint(1);
    return MyMath.ptDistanceToSegment(pt, p1, p2, null);
  }

  public EdObjectFactory getFactory() {
    return FACTORY;
  }

  public static EdObjectFactory FACTORY = new EdObjectFactory("s") {
    @Override
    public EdObject construct(Point defaultLocation) {
      EdSegment s = new EdSegment(null);
      if (defaultLocation != null) {
        s.addPoint(defaultLocation);
        s.addPoint(defaultLocation);
      }
      return s;
    }

    @Override
    public int getIconResource() {
      return R.raw.segmenticon;
    }
  };

  private static class EditorOperation extends UserOperation {

    public EditorOperation(Editor editor, int slot, int vertexNumber) {
      mEditor = editor;
      mEditSlot = slot;
      mEditPointIndex = vertexNumber;
      mCommand = new CommandForGeneralChanges(mEditor, FACTORY.getTag(), null);
    }

    @Override
    public void processUserEvent(UserEvent event) {

      switch (event.getCode()) {

      case UserEvent.CODE_DRAG: {
        // Create a new copy of the segment, with modified endpoint
        EdSegment segment = mEditor.objects().get(mEditSlot);
        segment = mutableCopyOf(segment);
        segment.setPoint(mEditPointIndex, event.getWorldLocation());
        mEditor.objects().set(mEditSlot, segment);
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

    // Index of object being edited
    private int mEditSlot;
    // Index of vertex being edited
    private int mEditPointIndex;
    private boolean mModified;
    private CommandForGeneralChanges mCommand;
    private Editor mEditor;
  }
}
