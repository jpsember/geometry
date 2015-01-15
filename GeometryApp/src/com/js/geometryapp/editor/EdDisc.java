package com.js.geometryapp.editor;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.basic.Freezable;
import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Disc;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.geometry.Rect;
import static com.js.basic.Tools.*;

public class EdDisc extends EdObject {

  private EdDisc(EdDisc source) {
    super(source);
  }

  @Override
  public Freezable getMutableCopy() {
    return new EdDisc(this);
  }

  @Override
  public boolean valid() {
    return nPoints() == 2;
  }

  @Override
  public Rect getBounds() {
    // The bounds is not just the smallest rect containing the two points,
    // but is instead the square containing the disc they represent
    float radius = getRadius();
    Point origin = getOrigin();
    Rect r = new Rect(origin.x - radius, origin.y - radius, radius * 2,
        radius * 2);
    return r;
  }

  @Override
  public void setPoint(int index, Point point) {
    // We must interpret a new second point as adjusting the radius
    if (index != 1)
      super.setPoint(index, point);
    else
      setRadius(MyMath.distanceBetween(getOrigin(), point));
  }

  private Point calculateSecondaryPointForRadius(float r) {
    return MyMath.pointOnCircle(getOrigin(), MyMath.M_DEG * 90, r);
  }

  @Override
  public void render(AlgorithmStepper s, boolean selected, boolean editable) {
    s.setColor(Color.BLUE);
    s.render(buildDisc());
    super.render(s, selected, editable);
  }

  @Override
  public UserOperation buildEditOperation(int slot, UserEvent initialPress) {
    int vertexIndex = closestVertex(initialPress.getWorldLocation(), editor()
        .pickRadius());
    if (vertexIndex >= 0)
      return new EditorOperation(editor(), slot, vertexIndex);
    return null;
  }

  @Override
  public boolean intersects(Rect r) {
    return r.distanceFrom(getOrigin()) < getRadius();
  }

  public Disc buildDisc() {
    return new Disc(getOrigin(), getRadius());
  }

  public float distFrom(Point pt) {
    Point p1 = getPoint(0);
    float dist = MyMath.distanceBetween(pt, p1);
    float radius = getRadius();
    float dist2 = Math.abs(dist - radius);
    dist = Math.min(dist, dist2);
    return dist;
  }

  public float getRadius() {
    return getPoint(1).y - getOrigin().y;
  }

  private void setRadius(float r) {
    super.setPoint(1, calculateSecondaryPointForRadius(r));
  }

  public Point getOrigin() {
    return getPoint(0);
  }

  private void setOrigin(Point p) {
    setPoint(0, p);
  }

  public EdObjectFactory getFactory() {
    return FACTORY;
  }

  @Override
  public void applyTransform(Matrix m) {
    float radius = getRadius();
    Point origin = getOrigin();

    // Transform the left and bottom sides of the bounding square
    Point bottomLeft = new Point(origin.x - radius, origin.y - radius);
    Point bottomRight = new Point(origin.x + radius, bottomLeft.y);
    Point topLeft = new Point(bottomLeft.x, origin.y + radius);

    bottomLeft.apply(m);
    bottomRight.apply(m);
    topLeft.apply(m);

    // The new origin is the midpoint of the diagonal of the transformed
    // square
    Point newOrigin = MyMath.interpolateBetween(bottomRight, topLeft, .5f);
    // The new radius is the smaller of the distances from the origin to the
    // transformed sides
    float bottomDistance = MyMath.ptDistanceToLine(newOrigin, bottomLeft,
        bottomRight, null);
    float leftDistance = MyMath.ptDistanceToLine(newOrigin, bottomLeft,
        topLeft, null);
    float newRadius = Math.min(bottomDistance, leftDistance);
    setOrigin(newOrigin);
    setRadius(newRadius);
  }

  public static EdObjectFactory FACTORY = new EdObjectFactory("d") {
    @Override
    public EdObject construct(Point defaultLocation) {
      EdDisc s = new EdDisc(null);
      if (defaultLocation != null) {
        s.setOrigin(defaultLocation);
        s.setRadius(20);
      }
      return s;
    }

    @Override
    public int getIconResource() {
      return R.raw.discicon;
    }

    @Override
    public void parsePoints(EdObject destinationObject, org.json.JSONObject map)
        throws org.json.JSONException {
      EdDisc disc = (EdDisc) destinationObject;
      super.parsePoints(disc, map);
      if (disc.nPoints() == 2) {
        disc.setRadius(MyMath.distanceBetween(disc.getOrigin(),
            disc.getPoint(1)));
      }
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

      case UserEvent.CODE_DOWN:
        mOriginalDisc = mEditor.objects().get(mEditSlot);
        mInitialOffset = MyMath.subtract(
            mOriginalDisc.getPoint(mEditPointIndex), event.getWorldLocation());
        break;

      case UserEvent.CODE_DRAG: {
        Point adjustedLoc = MyMath
            .add(event.getWorldLocation(), mInitialOffset);
        EdDisc origDisc = mEditor.objects().get(mEditSlot);
        EdDisc disc = mutableCopyOf(origDisc);

        if (mEditPointIndex == 1) {
          disc.setRadius(MyMath.distanceBetween(adjustedLoc,
              mOriginalDisc.getOrigin()));
        } else {
          float radius = disc.getRadius();
          disc.setOrigin(adjustedLoc);
          disc.setRadius(radius);
        }
        mEditor.objects().set(mEditSlot, disc);
        mModified = true;
      }
        break;

      case UserEvent.CODE_UP:
        if (mModified) {
          mCommand.finish();
        }
        event.clearOperation();
        break;
      }
    }

    private int mEditSlot;
    private int mEditPointIndex;
    private EdDisc mOriginalDisc;
    private Point mInitialOffset;
    private boolean mModified;
    private CommandForGeneralChanges mCommand;
    private Editor mEditor;
  }
}
