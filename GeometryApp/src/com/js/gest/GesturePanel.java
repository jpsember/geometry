package com.js.gest;

import java.util.HashMap;
import java.util.Map;

import com.js.basic.Point;
import com.js.basic.Rect;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.view.View;

public class GesturePanel {

  private static final float PADDING = 16.0f;
  private static final float MINIMIZED_HEIGHT = 32;

  /**
   * Constructor
   * 
   * @param container
   *          the View to contain the panel
   */
  public GesturePanel(View container) {
    mContainer = container;
  }

  /**
   * Draw the panel to a canvas; should be called by a View's onDraw() method
   */
  public void draw(Canvas canvas) {
    Rect r = getBounds();
    Paint paint = new Paint();
    paint.setColor(0x40808080);
    paint.setStrokeWidth(1.2f);

    fillRoundedRect(canvas, r, 16.0f, paint);
    drawStrokeSet(canvas);
  }

  private void drawStrokeSet(Canvas canvas) {
    if (mDisplayedStrokeSet == null)
      return;

    // If no scaled version exists, create one
    StrokeSet scaledSet = mScaledStrokeSets.get(mDisplayedStrokeSet.name());
    Rect r = new Rect(getBounds());
    final float STROKE_INSET = PADDING * 1.6f;
    r.inset(STROKE_INSET, STROKE_INSET);
    if (scaledSet == null) {
      scaledSet = mDisplayedStrokeSet.fitToRect(r);
      mScaledStrokeSets.put(mDisplayedStrokeSet.name(), scaledSet);
    }

    Paint paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(0x40505050);
    paint.setStrokeWidth(8f);

    Path path = mPath;
    path.reset();
    for (Stroke s : scaledSet) {
      Point ptPrev = null;
      for (int i = 0; i < s.size(); i++) {
        Point pt = s.getPoint(i);
        // Flip the stroke from its coordinate system to Android's
        pt = new Point(pt.x, r.endY() - pt.y + r.y);
        if (i == 0) {
          path.moveTo(pt.x, pt.y);
        } else if (i < s.size() - 1) {
          path.quadTo(ptPrev.x, ptPrev.y, (ptPrev.x + pt.x) / 2,
              (ptPrev.y + pt.y) / 2);
        } else {
          path.lineTo(pt.x, pt.y);
        }
        ptPrev = pt;
      }
    }
    canvas.drawPath(path, paint);
  }

  /**
   * Determine if the panel contains a point
   */
  public boolean containsPoint(Point point) {
    Rect r = getBounds();
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

  private Rect getBounds() {
    if (mViewBoundsNormal == null) {
      View view = mContainer;

      float minWidth = 340;
      float minHeight = 240;

      float width = Math.min(view.getWidth(), minWidth);
      float height = Math.min(view.getHeight(), minHeight);

      Rect rect = new Rect(view.getWidth() - width, view.getHeight() - height,
          width, height);

      rect.inset(PADDING, PADDING);
      mViewBoundsNormal = rect;

      mViewBoundsMinimized = new Rect(rect.x, rect.endY() - MINIMIZED_HEIGHT,
          rect.width, MINIMIZED_HEIGHT);
    }
    return isMinimized() ? mViewBoundsMinimized : mViewBoundsNormal;
  }

  public boolean isMinimized() {
    return mMinimized;
  }

  public void setMinimized(boolean state) {
    if (mMinimized == state)
      return;

    mMinimized = state;
    mContainer.invalidate();
    if (mMinimized) {
      mDisplayedStrokeSet = null;
    }
  }

  public void setGesture(StrokeSet strokeSet) {
    final float ERASE_GESTURE_DELAY = 1.2f;

    if (mDisplayedStrokeSet == strokeSet)
      return;
    mUniqueGestureNumber++;
    mDisplayedStrokeSet = strokeSet;
    if (!isMinimized()) {
      mContainer.invalidate();

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
  }

  private Path mPath = new Path();
  private Rect mViewBoundsNormal;
  private Rect mViewBoundsMinimized;
  private View mContainer;
  private boolean mMinimized;
  private StrokeSet mDisplayedStrokeSet;
  private Map<String, StrokeSet> mScaledStrokeSets = new HashMap();
  private Handler mHandler = new Handler();
  private int mUniqueGestureNumber;
}
