package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.AlgorithmStepper;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.geometry.R;
import com.js.basic.Rect;
import com.js.geometry.Sprite;
import static com.js.basic.Tools.*;

public class ScaleOperation extends UserOperation {

  private static final int NUM_HANDLES = 8;

  public ScaleOperation(Editor editor) {
    mEditor = editor;
    prepareScaleOperation();
  }

  @Override
  public void processUserEvent(UserEvent event) {

    switch (event.getCode()) {

    case UserEvent.CODE_DOWN: {
      prepareScaleOperation();
      // Find handle
      float minDist = 0;
      int minHandle = -1;
      Point minHandleLocation = null;
      for (int i = 0; i < NUM_HANDLES; i++) {
        Point handleLoc = handleBaseLocation(i, true);
        float dist = MyMath
            .distanceBetween(event.getWorldLocation(), handleLoc);
        if (minHandle < 0 || dist < minDist) {
          minDist = dist;
          minHandle = i;
          minHandleLocation = handleLoc;
        }
      }
      if (minDist > mEditor.pickRadius()) {
        event.clearOperation();
        break;
      }
      mActiveHandle = minHandle;
      mInitialHandleOffset = MyMath.subtract(minHandleLocation,
          event.getWorldLocation());
    }
      break;

    case UserEvent.CODE_DRAG:
      if (!mPerformingDragSequence)
        break;
      performScale(mActiveHandle, event.getWorldLocation());
      break;

    case UserEvent.CODE_UP: {
      if (!mPerformingDragSequence)
        break;
      mCommand.finish();
      setUnprepared();
    }
      break;
    }

  }

  @Override
  public void paint() {
    // Render with emphasis if scaled rect = original rect
    boolean equalsOriginal = mRect.equals(mScaledRect);
    AlgorithmStepper s = mEditor.stepper();
    if (equalsOriginal)
      s.setColor(Color.argb(0xff, 0xff, 0x40, 0x40));
    else
      s.setColor(Color.argb(0x80, 0xff, 0x40, 0x40));

    // Calculate handles corresponding to scaled rect
    List<Point> handles = new ArrayList();
    calculateHandleBaseLocations(mScaledRect, handles);
    for (int i = 0; i < sLinesBetweenHandles.length; i += 2)
      s.renderLine(handles.get(sLinesBetweenHandles[i]),
          handles.get(sLinesBetweenHandles[i + 1]));
    for (int i = 0; i < NUM_HANDLES; i++) {
      s.render(new Sprite(R.raw.squareicon, applyHandleExternalPadding(
          handles.get(i), i, true)));
    }
  }

  @Override
  public boolean allowEditableObject() {
    return false;
  }

  private static final int[] sLinesBetweenHandles = { 0, 2, 2, 4, 4, 6, 6, 0, };

  /**
   * Calculate the bounding rectangle for a set of objects
   * 
   * @param objects
   * @return bounding rectangle, or null if there are no objects
   */
  private Rect boundsForObjects(EdObjectArray objects) {
    Rect bounds = null;
    for (EdObject obj : objects) {
      Rect objBounds = obj.getBounds();
      if (bounds == null)
        bounds = objBounds;
      else
        bounds.include(objBounds);
    }
    mEditor.expandRectByPickRadius(bounds);
    return bounds;
  }

  private void prepareScaleOperation() {
    if (!mPerformingDragSequence) {
      mCommand = new CommandForGeneralChanges(mEditor, "scale", "Scale");
      mHandles = new ArrayList();
      // Don't replace an existing bounding rectangle, since it may have
      // been derived from a previous scale procedure involving these
      // objects, and recalculating it may produce a different rectangle
      // which can be disorienting to the user.
      if (mRect == null) {
        mRect = boundsForObjects(mEditor.objects().getSelectedObjects());
        // ensure rectangle isn't degenerate
        final float MIN_DIM = 1;
        Point mid = mRect.midPoint();
        if (mRect.width <= MIN_DIM) {
          mRect.x = mid.x - MIN_DIM;
          mRect.width = MIN_DIM;
        }
        if (mRect.height <= MIN_DIM) {
          mRect.y = mid.y - MIN_DIM;
          mRect.height = MIN_DIM;
        }
      }
      mScaledRect = mRect;
      calculateHandleBaseLocations(mRect, mHandles);
      mPerformingDragSequence = true;
    }
  }

  private void setUnprepared() {
    if (mPerformingDragSequence) {
      // Set the original (unscaled) rect equal to the previous
      // operation's scaled rect, so it doesn't get recalculated and
      // change its appearance disconcertingly.
      mRect = mScaledRect;
      mPerformingDragSequence = false;
    }
  }

  /**
   * Calculate handle base locations for a particular bounding rectangle.
   * 
   * A 'handle' is represented by an icon that the user can grab to perform a
   * scale operation. Each handle has a 'base location' which corresponds to its
   * location before scaling begins.
   */
  private void calculateHandleBaseLocations(Rect boundingRect,
      List<Point> handleLocations) {
    Point p0 = new Point(boundingRect.x, boundingRect.y);
    Point p1 = new Point(boundingRect.midX(), p0.y);
    Point p2 = new Point(boundingRect.endX(), p0.y);
    Point p3 = new Point(p2.x, boundingRect.midY());
    Point p4 = new Point(p2.x, boundingRect.endY());
    Point p5 = new Point(p1.x, p4.y);
    Point p6 = new Point(p0.x, p4.y);
    Point p7 = new Point(p0.x, p3.y);
    handleLocations.clear();
    handleLocations.add(p0);
    handleLocations.add(p1);
    handleLocations.add(p2);
    handleLocations.add(p3);
    handleLocations.add(p4);
    handleLocations.add(p5);
    handleLocations.add(p6);
    handleLocations.add(p7);
  }

  /**
   * Adjust the location of a handle to move it between its actual location (on
   * the bounding rect boundary) and its displayed location (some small distance
   * outside the rect). This adjustment allows the handles to appear
   * well-separated even if bounding rect is small (or degenerate).
   * 
   * @param location
   *          location of handle (actual vs displayed)
   * @param handleIndex
   * @param addFlag
   *          true to adjust actual->displayed; false for displayed->actual
   */
  private Point applyHandleExternalPadding(Point location, int handleIndex,
      boolean addFlag) {
    Point adjustedLocation = new Point(location);
    float sign = addFlag ? 1 : -1;
    float p = mEditor.pickRadius() * .5f * sign;
    if (handleIndex <= 2)
      adjustedLocation.y -= p;
    else if (handleIndex >= 4 && handleIndex <= 6)
      adjustedLocation.y += p;
    if (handleIndex == 0 || handleIndex >= 6)
      adjustedLocation.x -= p;
    else if (handleIndex >= 2 && handleIndex <= 4)
      adjustedLocation.x += p;
    return adjustedLocation;
  }

  /**
   * Filter user's touch location to be a valid new location for a particular
   * handle
   * 
   * @param handle
   *          index of handle being adjusted
   * @param touchLocation
   *          user's touch location
   */
  private Point filteredHandle(int handle, Point touchLocation) {
    touchLocation = MyMath.add(touchLocation, mInitialHandleOffset);
    touchLocation = applyHandleExternalPadding(touchLocation, handle, false);

    float x0 = touchLocation.x;
    float x1 = touchLocation.x;
    float y0 = touchLocation.y;
    float y1 = touchLocation.y;

    // Add some padding to stop the scaled rectangle from becoming
    // degenerate
    float padding = Math.min(mRect.minDim() / 2, mEditor.pickRadius() * .3f);
    if (handle == 0 || handle == 1 || handle == 2)
      y1 = mRect.midY() - padding;
    if (handle == 2 || handle == 3 || handle == 4)
      x0 = mRect.midX() + padding;
    if (handle == 4 || handle == 5 || handle == 6)
      y0 = mRect.midY() + padding;
    if (handle == 6 || handle == 7 || handle == 0)
      x1 = mRect.midX() - padding;
    touchLocation.setTo(MyMath.clamp(touchLocation.x, x0, x1),
        MyMath.clamp(touchLocation.y, y0, y1));

    // Replace new handle location with its projection to the line between
    // the handle and the midpoint
    Point origin = mRect.midPoint();
    Point filtered = new Point();
    Point handleBase = handleBaseLocation(handle, false);
    MyMath.ptDistanceToLine(touchLocation, handleBase, origin, filtered);
    return filtered;
  }

  private Point handleBaseLocation(int handleIndex, boolean applyExternalPadding) {
    Point loc = mHandles.get(handleIndex);
    if (applyExternalPadding)
      loc = applyHandleExternalPadding(loc, handleIndex, true);
    return loc;
  }

  /**
   * Calculate the scaled rectangle corresponding to a particular handle
   * location
   */
  private Rect calculateScaledRect(int handle, Point handleLocation) {
    Point origin = mRect.midPoint();
    float w = mRect.width / 2;
    float h = mRect.height / 2;
    if (handle != 1 && handle != 5)
      w = Math.abs(origin.x - handleLocation.x);
    if (handle != 3 && handle != 7)
      h = Math.abs(origin.y - handleLocation.y);
    return new Rect(origin.x - w, origin.y - h, w * 2, h * 2);
  }

  private void performScale(int handle, Point touchLocation) {
    touchLocation = filteredHandle(handle, touchLocation);
    mScaledRect = calculateScaledRect(handle, touchLocation);

    // Act as if there's a 'groove' at the original (unscaled) rectangle
    // location: If scaled rectangle is very close to original, set it
    // exactly equal to it. We don't want the groove to be too large,
    // because this prevents small changes and is frustrating
    float diff = Math.max(Math.abs(mScaledRect.width - mRect.width),
        Math.abs(mScaledRect.height - mRect.height)) / 2;
    if (diff < mEditor.pickRadius() * .05f) {
      mScaledRect = mRect;
    }
    scaleObjects();
  }

  /**
   * Calculate transform to scale objects relative to their bounding rect's
   * center
   */
  private Matrix calcScaleTransform() {
    Matrix matrix = new Matrix();
    Matrix matrix2 = new Matrix();
    Point origin = mRect.midPoint();

    matrix2.setTranslate(-origin.x, -origin.y);
    matrix.postConcat(matrix2);
    matrix2.setScale(mScaledRect.width / mRect.width, mScaledRect.height
        / mRect.height);
    matrix.postConcat(matrix2);
    matrix2.setTranslate(origin.x, origin.y);
    matrix.postConcat(matrix2);
    return matrix;
  }

  /**
   * Replace selected objects with scaled counterparts
   */
  private void scaleObjects() {
    Matrix matrix = calcScaleTransform();
    EditorState state = mCommand.getOriginalState();
    for (int slot : state.getSelectedSlots()) {
      EdObject object = state.getObjects().get(slot);
      EdObject scaled = mutableCopyOf(object);
      scaled.applyTransform(matrix);
      mEditor.objects().set(slot, scaled);
    }
  }

  private Editor mEditor;

  // True if processing a down/drag/up scaling operation
  private boolean mPerformingDragSequence;
  private CommandForGeneralChanges mCommand;
  // Bounding rect of unscaled objects
  private Rect mRect;
  // Handle base locations for unscaled objects
  private List<Point> mHandles;
  // Scaled bounding rect
  private Rect mScaledRect;
  // Which handle the user is adjusting
  private int mActiveHandle;
  // amount to add to user touch location to place exactly at handle
  private Point mInitialHandleOffset;
}
