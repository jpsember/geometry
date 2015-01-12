package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;

import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Disc;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.geometry.Sprite;

import static com.js.basic.Tools.*;

public class EdPolyline extends EdObject {

  private static final float ABSORBTION_FACTOR_NORMAL = 1.4f;
  private static final float ABSORBTION_FACTOR_WHILE_INSERTING = .3f;

  private static final int TAB_INSERT_BACKWARD = 0;
  private static final int TAB_INSERT_FORWARD = 1;
  private static final int TAB_TOTAL = 2;

  private static final int COLOR_SIGNAL_GREEN = Color.argb(0x40, 0x60, 0xff,
      0x60);

  private EdPolyline() {
  }

  @Override
  public boolean valid() {
    return nPoints() >= 1 && mCursor >= 0 && mCursor < nPoints();
  }

  @Override
  public void render(AlgorithmStepper s) {
    Point prev = null;
    if (closed() && nPoints() > 2)
      prev = getPointMod(-1);

    s.setColor(Color.BLUE);
    for (int i = 0; i < nPoints(); i++) {
      Point pt = getPoint(i);
      if (prev != null) {
        renderLine(s, prev, pt);
      }
      prev = pt;
    }
    super.render(s);
    // If there's a single point, we must plot it (if unselected) since no
    // segments were drawn
    if (!isSelected() && nPoints() == 1)
      s.render(getPoint(0));

    Point[] tabLocations = null;
    if (isEditable() && !mTabsHidden) {
      boolean interpFlags[] = new boolean[2];
      tabLocations = calculateTabPositions(this, interpFlags);
      Point cursor = getPoint(mCursor);
      s.setColor(Color.GRAY);
      for (int i = 0; i < 2; i++) {
        Point pt = tabLocations[i];
        if (pt == null)
          continue;
        if (interpFlags[i])
          continue;
        s.renderLine(cursor, pt);
      }
      for (Point pt : tabLocations) {
        if (pt == null)
          continue;
        s.render(new Sprite(R.raw.squareicon, pt));
      }
    }
  }

  private boolean targetWithinTab(Point target, Point tabLocation) {
    boolean within = false;
    if (tabLocation != null) {
      float dist = MyMath.distanceBetween(target, tabLocation);
      within = (dist <= editor().pickRadius());
    }
    return within;
  }

  @Override
  public String toString() {
    if (!DEBUG_ONLY_FEATURES)
      return "";

    StringBuilder sb = new StringBuilder("EdPolyline");
    sb.append(closed() ? " CL" : " OP");
    sb.append(" c=" + d(cursor(), 2));
    sb.append(" [");
    for (int i = 0; i < nPoints(); i++) {
      Point pt = getPoint(i);
      if (i == cursor())
        sb.append("   *");
      else
        sb.append("    ");
      sb.append(d((int) pt.x, 3));
      sb.append(',');
      sb.append(d((int) pt.y, 3));
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public UserOperation buildEditOperation(int slot, UserEvent initialPress) {
    Point location = initialPress.getWorldLocation();
    Point[] tabLocations = calculateTabPositions(this, null);

    if (targetWithinTab(location, tabLocations[TAB_INSERT_FORWARD])) {
      EdPolyline mod = getCopy();
      // Insert a new vertex after the cursor
      mod.mCursor++;
      mod.addPoint(mod.mCursor, location);
      if (mod.closed()) {
        mod.rotateVertexPositions(-1);
        mod.setClosed(false);
      }
      return OurOperation.buildInsertOperation(editor(), slot, mod);
    }

    if (targetWithinTab(location, tabLocations[TAB_INSERT_BACKWARD])) {
      EdPolyline mod = getCopy();
      // Insert a new vertex before the cursor
      mod.addPoint(mod.mCursor, location);
      if (mod.closed()) {
        mod.rotateVertexPositions(0);
        mod.setClosed(false);
      }
      return OurOperation.buildInsertOperation(editor(), slot, mod);
    }

    int vertexIndex = closestVertex(location, editor().pickRadius());
    if (vertexIndex >= 0) {
      mCursor = vertexIndex;
      EdPolyline mod = getCopy();
      mod.mCursor = vertexIndex;
      return OurOperation.buildMoveOperation(editor(), slot, mod);
    }
    return null;
  }

  @Override
  public float distFrom(Point targetPoint) {
    Point prev = null;
    float minDistance = MyMath.distanceBetween(targetPoint, getPoint(0));
    int max = closed() ? nPoints() : nPoints() - 1;
    for (int i = 0; i <= max; i++) {
      Point pt = getPointMod(i);
      if (prev != null) {
        float distance = MyMath
            .ptDistanceToSegment(targetPoint, prev, pt, null);
        if (minDistance < 0)
          minDistance = distance;
        minDistance = Math.min(minDistance, distance);
      }
      prev = pt;
    }
    return minDistance;
  }

  @Override
  public void selectedForEditing(Point location) {
    int vertexIndex = closestVertex(location, editor().pickRadius());
    if (vertexIndex >= 0) {
      setCursor(vertexIndex);
    }
  }

  public EdObjectFactory getFactory() {
    return FACTORY;
  }

  public static EdObjectFactory FACTORY = new EdObjectFactory("P") {

    private static final String JSON_KEY_CURSOR = "c";
    private static final String JSON_KEY_CLOSED = "cl";

    public EdObject construct(Point defaultLocation) {
      EdPolyline p = new EdPolyline();
      if (defaultLocation != null) {
        p.addPoint(defaultLocation);
        p.addPoint(defaultLocation);
      }
      return p;
    }

    @Override
    public JSONObject write(EdObject obj) throws JSONException {
      EdPolyline p = (EdPolyline) obj;
      JSONObject map = super.write(obj);
      map.put(JSON_KEY_CURSOR, p.cursor());
      map.put(JSON_KEY_CLOSED, p.closed());
      return map;
    }

    @Override
    public EdPolyline parse(JSONObject map) throws JSONException {
      EdPolyline p = super.parse(map);
      // TODO: is it necessary to persist the cursor?
      p.setCursor(map.optInt(JSON_KEY_CURSOR));
      p.setClosed(map.optBoolean(JSON_KEY_CLOSED));
      return p;
    };

    @Override
    public int getIconResource() {
      return R.raw.polylineicon;
    };
  };

  private void setCursor(int c) {
    mCursor = c;
  }

  public int cursor() {
    return mCursor;
  }

  public boolean closed() {
    return mClosed;
  }

  public void setClosed(boolean f) {
    mClosed = f;
  }

  private static float calcAngle(Point p1, Point p2) {
    if (MyMath.distanceBetween(p1, p2) < 5) {
      return 0;
    }
    return MyMath.polarAngleOfSegment(p1, p2);
  }

  private void setTabsHidden(boolean f) {
    mTabsHidden = f;
  }

  /**
   * Shift this polyline's vertex positions around
   * 
   * @param newCursorPosition
   *          position of cursor vertex after shift
   */
  private void rotateVertexPositions(int newCursorPosition) {
    newCursorPosition = MyMath.myMod(newCursorPosition, nPoints());
    int delta = MyMath.myMod(newCursorPosition - cursor(), nPoints());
    if (delta == 0)
      return;
    List<Point> v = new ArrayList();
    for (int i = 0; i < nPoints(); i++)
      v.add(getPoint(i));
    Collections.rotate(v, delta);
    for (int i = 0; i < nPoints(); i++)
      setPoint(i, v.get(i));
    setCursor(newCursorPosition);
  }

  /**
   * Calculate a reasonable place to put the insert vertex tabs for an editable
   * polyline
   * 
   * @param polyline
   * @param interpolateFlags
   *          if not null, an array of two booleans; each will be set true iff
   *          the corresponding tab lies on neighboring segment
   * @return array of TAB_TOTAL tab locations
   */
  private static Point[] calculateTabPositions(EdPolyline polyline,
      boolean[] interpolateFlags) {
    Point[] tabLocations = new Point[TAB_TOTAL];
    if (interpolateFlags == null)
      interpolateFlags = new boolean[2];
    interpolateFlags[0] = false;
    interpolateFlags[1] = false;

    int c = polyline.cursor();

    float tabDistance = polyline.editor().pickRadius() * 2.5f;

    float a1 = 0, a2 = 0;
    boolean convex = true;
    Point pa = null;
    Point pb = polyline.getPoint(c);
    Point pc = null;
    if (c > 0 || polyline.closed())
      pa = polyline.getPointMod(c - 1);
    if (c + 1 < polyline.nPoints() || polyline.closed())
      pc = polyline.getPointMod(c + 1);

    if (pa != null) {
      a1 = calcAngle(pa, pb);
      if (MyMath.distanceBetween(pa, pb) >= tabDistance * 1.2f)
        interpolateFlags[0] = true;
    }
    if (pc != null) {
      a2 = calcAngle(pb, pc);
      if (MyMath.distanceBetween(pb, pc) >= tabDistance * 1.2f)
        interpolateFlags[1] = true;
    }

    if (pa == null)
      a1 = a2;
    if (pc == null)
      a2 = a1;

    final float TAB_MIN_ANGULAR_SEPARATION = MyMath.M_DEG * 45;
    {
      // If tab arms are too close together, adjust them
      float separation = MyMath.normalizeAngle(a2 - a1);
      float clampedSeparation = MyMath.clamp(separation,
          -(MyMath.PI - TAB_MIN_ANGULAR_SEPARATION), MyMath.PI
              - TAB_MIN_ANGULAR_SEPARATION);
      float diff = clampedSeparation - separation;
      a2 += diff / 2;
      a1 -= diff / 2;
      if (diff != 0) {
        interpolateFlags[0] = false;
        interpolateFlags[1] = false;
      }
    }

    if (pa != null && pc != null) {
      convex = MyMath.sideOfLine(pa, pb, pc) > 0;
    }

    final float TAB_ROTATION_FROM_ADJACENT_SEGMENTS = MyMath.M_DEG * 18;

    float tabRotationFactor = TAB_ROTATION_FROM_ADJACENT_SEGMENTS;
    if (!convex) {
      tabRotationFactor = -tabRotationFactor;
    }
    a1 += tabRotationFactor + MyMath.PI;
    a2 -= tabRotationFactor;

    if (interpolateFlags[0])
      tabLocations[TAB_INSERT_BACKWARD] = MyMath
          .interpolateBetween(pa, pb, .5f);
    else
      tabLocations[TAB_INSERT_BACKWARD] = MyMath.pointOnCircle(pb, a1,
          tabDistance);

    if (interpolateFlags[1])
      tabLocations[TAB_INSERT_FORWARD] = MyMath.interpolateBetween(pb, pc, .5f);
    else
      tabLocations[TAB_INSERT_FORWARD] = MyMath.pointOnCircle(pb, a2,
          tabDistance);

    return tabLocations;
  }

  private boolean mClosed;

  // information concerning editable object
  private int mCursor;
  private boolean mTabsHidden;

  private static class OurOperation extends UserOperation {

    private final static int OPER_MOVE = 0;
    private final static int OPER_INSERT = 1;

    /**
     * Constructor
     * 
     * @param editor
     * @param slot
     *          slot containing object being edited
     * @param modified
     *          new object
     * @param vertexNumber
     *          vertex number being edited
     */
    private OurOperation(Editor editor, int slot, EdPolyline modified,
        int operType) {
      mOperType = operType;
      mEditor = editor;
      mEditSlot = slot;
      mReference = modified;
      mOriginalState = new EditorState(editor);
      mOriginalPolyline = mOriginalState.getObjects().get(mEditSlot);
      editor.objects().set(slot, mReference);
    }

    private EdPolyline activePolyline() {
      return mEditor.objects().get(mEditSlot);
    }

    public static UserOperation buildMoveOperation(Editor editor, int slot,
        EdPolyline mod) {
      return new OurOperation(editor, slot, mod, OPER_MOVE);
    }

    public static UserOperation buildInsertOperation(Editor editor, int slot,
        EdPolyline mod) {
      return new OurOperation(editor, slot, mod, OPER_INSERT);
    }

    @Override
    public void processUserEvent(UserEvent event) {

      switch (event.getCode()) {

      case UserEvent.CODE_DRAG: {
        // Create a new copy of the polyline, with modified endpoint
        EdPolyline polyline = mReference.getCopy();
        mEditor.objects().set(mEditSlot, polyline);
        polyline.setTabsHidden(true);
        {
          mChangesMade = true;
          polyline.setPoint(polyline.cursor(), event.getWorldLocation());
          int absorbVertex = findAbsorbingVertex(polyline);
          mSignal = (absorbVertex >= 0);
          if (mSignal) {
            polyline.setPoint(polyline.cursor(),
                polyline.getPoint(absorbVertex));
          }
        }
      }
        break;

      case UserEvent.CODE_UP:
        if (mChangesMade) {
          EdPolyline polyline = activePolyline();
          int absVert = findAbsorbingVertex(polyline);
          if (absVert >= 0) {
            performAbsorption(polyline, absVert);
          }
          // Don't allow any merging with polygon commands, because
          // the user may end up doing a lot of work on a single
          // polygon and he should be able to undo individual steps
          mEditor.pushCommand(new CommandForGeneralChanges(mEditor,
              mOriginalState, null, null, null));
        }
        event.clearOperation();
        break;

      }
    }

    @Override
    public void stop() {
      activePolyline().setTabsHidden(false);

      // Check if polyline has only two vertices very close together.
      // This can happen when the user clicks when adding a new
      // polyline
      // instead of dragging.
      // Delete the second one if this occurs.
      EdPolyline polyline = activePolyline();
      if (polyline.nPoints() == 2
          && MyMath.distanceBetween(polyline.getPoint(0), polyline.getPoint(1)) < 5) {
        polyline.removePoint(1);
        polyline.setCursor(0);
      }
    }

    /**
     * Determine which vertex, if any, is close enough to absorb the cursor's
     * 
     * @param p
     *          polyline
     * @param factor
     * @return index of absorbing vertex, or -1
     */
    private int findAbsorbingVertex(EdPolyline p) {
      if (p.nPoints() <= 1)
        return -1;

      Point cp = p.getPoint(p.cursor());
      for (int pass = 0; pass < 2; pass++) {
        int delta = (pass == 0) ? -1 : 1;
        int neighbor = MyMath.myMod(p.cursor() + delta, p.nPoints());
        Point c2 = p.getPoint(neighbor);
        float dist = MyMath.distanceBetween(cp, c2);
        // If we just split a closed polyline, don't assume he wants to
        // close it again; use smaller radius
        boolean closingVertex = !p.closed() && !mOriginalPolyline.closed()
            && Math.abs(neighbor - p.cursor()) > 1;
        float factor = absorptionFactor(!closingVertex
            && mOperType == OPER_INSERT);
        if (dist >= mEditor.pickRadius() * factor)
          continue;
        return neighbor;
      }
      return -1;
    }

    private void performAbsorption(EdPolyline p, int absorberIndex) {
      int removedIndex = p.cursor();
      // Delete the cursor vertex
      p.removePoint(removedIndex);
      // Check if we're closing an open polygon by this procedure
      boolean closingFlag = Math.abs(removedIndex - absorberIndex) > 1;
      if (closingFlag)
        p.setClosed(closingFlag);
      p.setCursor(removedIndex < absorberIndex ? absorberIndex - 1
          : absorberIndex);
    }

    @Override
    public void paint() {
      AlgorithmStepper s = mEditor.stepper();
      if (mSignal) {
        EdPolyline polyline = activePolyline();
        Point signalLocation = polyline.getPoint(polyline.cursor());
        s.setColor(COLOR_SIGNAL_GREEN);
        s.render(new Disc(signalLocation, 15).renderable(true));
      }
    }

    private float absorptionFactor(boolean small) {
      // The 'absorbing' vertices factor is much smaller when inserting;
      // this allows the user to place vertices very close together if
      // they desire.
      return small ? ABSORBTION_FACTOR_WHILE_INSERTING
          : ABSORBTION_FACTOR_NORMAL;
    }

    // Index of object being edited
    private Editor mEditor;
    private int mEditSlot;
    private EditorState mOriginalState;
    // polyline before editing operation began
    private EdPolyline mOriginalPolyline;
    // polyline just after editing operation began
    private EdPolyline mReference;
    private boolean mChangesMade;
    private boolean mSignal;
    private int mOperType;
  }

}
