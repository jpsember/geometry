package com.js.gest;

import java.util.HashMap;
import java.util.Map;

import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.basic.Rect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.view.View;

public class GesturePanel extends View {

  private static final float PADDING = 8.0f;

  /**
   * Constructor
   */
  public GesturePanel(Context context) {
    super(context);
    mFilter = new GestureEventFilter(this);
  }

  public GestureEventFilter getFilter() {
    return mFilter;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    onDrawAux(canvas);
  }

  private void onDrawAux(Canvas canvas) {
    Rect r = getActiveBounds();
    Paint paint = new Paint();
    paint.setColor(0x40808080);
    paint.setStrokeWidth(1.2f);
    fillRoundedRect(canvas, r, 16.0f, paint);
    drawStrokeSet(canvas);
  }

  /**
   * Transform a stroke point from its coordinate system (origin bottom left) to
   * Android's (origin top left) by flipping the y coordinate
   * 
   * @param bounds
   * @param pt
   */
  private Point flipVertically(Rect bounds, Point pt) {
    return new Point(pt.x, bounds.endY() - pt.y + bounds.y);
  }

  private void drawStrokeSet(Canvas canvas) {
    if (mDisplayedStrokeSet == null)
      return;

    // If no scaled version exists, create one
    StrokeSet scaledSet = mScaledStrokeSets.get(mDisplayedStrokeSet.name());
    Rect r = new Rect(getActiveBounds());
    final float STROKE_INSET = PADDING * 1.8f;
    r.inset(STROKE_INSET, STROKE_INSET);
    if (scaledSet == null) {
      scaledSet = mDisplayedStrokeSet.fitToRect(r);
      mScaledStrokeSets.put(mDisplayedStrokeSet.name(), scaledSet);
    }

    Paint paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0xffa0a0a0);
    paint.setStrokeWidth(8f);

    for (Stroke s : scaledSet) {
      Path path = mPath;
      path.reset();
      Point ptPrev1 = null;
      Point ptPrev2 = null;
      for (int i = 0; i < s.size(); i++) {
        Point pt = s.getPoint(i);
        pt = flipVertically(r, pt);
        if (i == 0) {
          path.moveTo(pt.x, pt.y);
        } else if (i < s.size() - 1) {
          ptPrev1 = new Point((ptPrev2.x + pt.x) / 2, (ptPrev2.y + pt.y) / 2);
          path.quadTo(ptPrev2.x, ptPrev2.y, ptPrev1.x, ptPrev1.y);
        } else {
          path.lineTo(pt.x, pt.y);
        }
        ptPrev2 = pt;
      }
      canvas.drawPath(path, paint);

      if (scaledSet.isDirected() && ptPrev1 != null) {
        float angle = MyMath.polarAngleOfSegment(ptPrev1, ptPrev2) + MyMath.PI;
        float arrowheadLength = r.maxDim() * .08f;
        final float ARROWHEAD_ANGLE = MyMath.M_DEG * 22;
        Point rightFlange = MyMath.pointOnCircle(ptPrev2, angle
            - ARROWHEAD_ANGLE, arrowheadLength);
        Point leftFlange = MyMath.pointOnCircle(ptPrev2, angle
            + ARROWHEAD_ANGLE, arrowheadLength);
        path.reset();
        path.moveTo(rightFlange.x, rightFlange.y);
        path.lineTo(ptPrev2.x, ptPrev2.y);
        path.lineTo(leftFlange.x, leftFlange.y);
        canvas.drawPath(path, paint);
      }
    }
  }

  /**
   * Determine if the panel contains a point
   */
  public boolean containsPoint(Point point) {
    Rect r = getActiveBounds();
    return r.contains(point);
  }

  private void fillRoundedRect(Canvas canvas, Rect rect, float radius,
      Paint paint) {
    Path path = mPath;
    path.reset();
    path.moveTo(rect.x + radius, rect.y);
    path.lineTo(rect.endX() - radius, rect.y);
    path.quadTo(rect.endX(), rect.y, rect.endX(), rect.y + radius);
    path.lineTo(rect.endX(), rect.endY() - radius);
    path.quadTo(rect.endX(), rect.endY(), rect.endX() - radius, rect.endY());
    path.lineTo(rect.x + radius, rect.endY());
    path.quadTo(rect.x, rect.endY(), rect.x, rect.endY() - radius);
    path.lineTo(rect.x, rect.y + radius);
    path.quadTo(rect.x, rect.y, rect.x + radius, rect.y);
    canvas.drawPath(path, paint);
  }

  private Rect getActiveBounds() {

    float width = getWidth();
    float height = getHeight();

    Rect rect = new Rect(0, 0, width, height);
    rect.inset(PADDING, PADDING);

    return rect;
  }

  public void setGesture(StrokeSet strokeSet) {
    final float ERASE_GESTURE_DELAY = 1.2f;

    if (mDisplayedStrokeSet == strokeSet)
      return;
    mUniqueGestureNumber++;
    mDisplayedStrokeSet = strokeSet;
    invalidate();

    // If we've set a gesture, set timer to erase it after a second or two
    if (strokeSet != null) {
      // Don't erase a more recently plotted gesture!
      // Make sure this task corresponds to the unique instance we
      // want to erase.
      final int gestureToErase = mUniqueGestureNumber;
      mHandler.postDelayed(new Runnable() {
        public void run() {
          if (mUniqueGestureNumber == gestureToErase) {
            setGesture(null);
          }
        }
      }, (long) (ERASE_GESTURE_DELAY * 1000));
    }
  }

  private Path mPath = new Path();
  private StrokeSet mDisplayedStrokeSet;
  private Map<String, StrokeSet> mScaledStrokeSets = new HashMap();
  private Handler mHandler = new Handler();
  private int mUniqueGestureNumber;
  private GestureEventFilter mFilter;
}
