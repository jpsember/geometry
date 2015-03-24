package com.js.gest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.basic.Rect;
import com.js.gest.GestureSet.Match;

import static com.js.basic.Tools.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public class GesturePanel extends View {

  private static final float PADDING = 8.0f;

  /**
   * Constructor
   */
  public GesturePanel(Context context) {
    super(context);
    this.setOnTouchListener(new OurTouchListener());
  }

  public void setListener(Listener listener) {
    mListener = listener;
  }

  public void setGestures(GestureSet c) {
    mStrokeSetCollection = c;
  }

  /**
   * Set which gesture is displayed; does nothing if in shared view mode
   * 
   * @param gestureName
   *          name to display, or null; if no such gesture exists, removes any
   *          existing gesture
   * @param substituteAlias
   *          if true, and named gesture has an alias, displays the alias
   *          instead
   */
  public void setDisplayedGesture(String gestureName, boolean substituteAlias) {
    StrokeSet set = null;
    if (gestureName != null) {
      set = mStrokeSetCollection.get(gestureName);
      if (set != null) {
        if (substituteAlias && set.hasAlias()) {
          set = mStrokeSetCollection.get(set.aliasName());
        }
      }
    }
    setGesture(set);
  }

  @Override
  public boolean performClick() {
    return super.performClick();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    onDrawAux(canvas);
  }

  private void onDrawAux(Canvas canvas) {
    Rect r = getActiveBounds();
    Paint paint = new Paint();
    paint.setColor(0xffe0e0e0);
    paint.setStrokeWidth(1.2f);
    fillRoundedRect(canvas, r, 16.0f, paint);
    drawStrokeSet(canvas);
  }

  /**
   * Transform a stroke point from its coordinate system (origin bottom left) to
   * Android's (origin top left) by flipping the y coordinate
   */
  private Point flipVertically(Point pt) {
    return new Point(pt.x, getHeight() - pt.y);
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
        Point pt = flipVertically(s.getPoint(i));
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

  private void setGesture(StrokeSet strokeSet) {
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

  private void performMatch(StrokeSet userStrokeSet) {
    if (mStrokeSetCollection == null) {
      warning("no stroke collection defined");
      return;
    }
    if (userStrokeSet.isTap()) {
      if (mListener != null)
        mListener.processGesture(GestureSet.GESTURE_TAP);
      return;
    }

    mMatch = null;
    StrokeSet set = userStrokeSet;
    set = set.fitToRect(null);
    set = set.normalize();

    ArrayList<GestureSet.Match> matches = new ArrayList();
    Match match = mStrokeSetCollection.findMatch(set, null, matches);
    if (match == null)
      return;
    // If the match cost is significantly less than the second best, use it
    if (matches.size() >= 2) {
      Match match2 = matches.get(1);
      if (match.cost() * 1.5f > match2.cost())
        return;
    }
    mMatch = match;
    mListener.processGesture(mMatch.strokeSet().aliasName());
    setDisplayedGesture(mMatch.strokeSet().name(), true);
  }

  public static interface Listener {

    /**
     * In normal use, this is the only method that has to do anything; the
     * client should handle the recognized gesture
     */
    void processGesture(String gestureName);

    /**
     * For development purposes only: called when stroke set has been received
     * from user, but before any matching has occurred
     * 
     * @param set
     *          a frozen, normalized stroke set
     */
    void processStrokeSet(StrokeSet set);

  }

  /**
   * Define a default Listener that does nothing, in case user hasn't specified
   * one
   */
  private static Listener DO_NOTHING_LISTENER = new Listener() {
    @Override
    public void processGesture(String gestureName) {
    }

    @Override
    public void processStrokeSet(StrokeSet set) {
      warning("No GesturePanel Listener defined");
    }
  };

  /**
   * Act as if user entered a particular stroke set
   */
  public void setEnteredStrokeSet(StrokeSet set) {
    set.freeze();
    mListener.processStrokeSet(set);
    performMatch(set);
  }

  /**
   * TouchListener for the GesturePanel
   */
  private class OurTouchListener implements OnTouchListener {

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      // Avoid Eclipse warnings:
      if (alwaysFalse())
        view.performClick();
      if (!mReceivingGesture
          && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        mReceivingGesture = true;
      }
      if (mReceivingGesture) {
        processGestureEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
          mReceivingGesture = false;
        }
      }
      return true;
    }

    private void processGestureEvent(MotionEvent event) {
      int actionMasked = event.getActionMasked();
      if (actionMasked == MotionEvent.ACTION_DOWN) {
        mStartEventTimeMillis = event.getEventTime();
        mTouchStrokeSet = new StrokeSet();
      }

      float eventTime = ((event.getEventTime() - mStartEventTimeMillis) / 1000.0f);

      int activeId = event.getPointerId(event.getActionIndex());
      MotionEvent.PointerCoords mCoord = new MotionEvent.PointerCoords();
      for (int i = 0; i < event.getPointerCount(); i++) {
        int ptrId = event.getPointerId(i);
        event.getPointerCoords(i, mCoord);
        Point pt = flipVertically(new Point(mCoord.x, mCoord.y));
        mTouchStrokeSet.addPoint(eventTime, ptrId, pt);
      }

      if (actionMasked == MotionEvent.ACTION_UP
          || actionMasked == MotionEvent.ACTION_POINTER_UP) {
        mTouchStrokeSet.stopStroke(activeId);
        if (!mTouchStrokeSet.areStrokesActive()) {
          setEnteredStrokeSet(mTouchStrokeSet);
        }
      }
    }

    // Stroke set from user touch event
    private StrokeSet mTouchStrokeSet;
    private long mStartEventTimeMillis;
    private boolean mReceivingGesture;
  }

  private Listener mListener = DO_NOTHING_LISTENER;
  private Path mPath = new Path();
  private StrokeSet mDisplayedStrokeSet;
  private Map<String, StrokeSet> mScaledStrokeSets = new HashMap();
  private Handler mHandler = new Handler();
  private int mUniqueGestureNumber;
  private GestureSet mStrokeSetCollection;
  private Match mMatch;
}
