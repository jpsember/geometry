package com.js.android;

import com.js.basic.MyMath;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import static com.js.basic.Tools.*;

public final class UITools {

  public static final boolean SET_DEBUG_COLORS = false && DEBUG_ONLY_FEATURES;

  private static int debugColors[] = {
      //
      // check out http://www.colorpicker.com/
      //
      0x10, 0x10, 0xe0, // dark blue
      0x37, 0x87, 0x3E, // dark green
      0x73, 0x5E, 0x22, // brown
      0xC7, 0x32, 0x00, // dark red
      0x8C, 0x26, 0xBF, // purple
      0x82, 0xB6, 0xBA, // blue/gray
      0xA3, 0x62, 0x84, // plum
      0xC7, 0x92, 0x00, // burnt orange
  };

  public static int debugColor() {
    return debugColor(sDebugColorIndex++);
  }

  public static int debugColor(int index) {
    index = MyMath.myMod(index, debugColors.length / 3) * 3;
    return Color.argb(255, debugColors[index], debugColors[index + 1],
        debugColors[index + 2]);
  }

  public static void applyDebugColors(View view) {
    if (SET_DEBUG_COLORS)
      view.setBackgroundColor(debugColor());
  }

  /**
   * Construct a LinearLayout
   * 
   * @param context
   * @param vertical
   *          true if it is to have a vertical orientation
   */
  public static LinearLayout linearLayout(Context context, boolean vertical) {
    LinearLayout view = new LinearLayout(context);
    view.setOrientation(vertical ? LinearLayout.VERTICAL
        : LinearLayout.HORIZONTAL);
    // Give view a minimum size in each dimension, so we are more likely to
    // detect problems with the layout (e.g. views not showing up)
    view.setMinimumWidth(15);
    view.setMinimumHeight(15);
    UITools.applyDebugColors(view);
    return view;
  }

  /**
   * Construct LayoutParams for child views of a LinearLayout container with a
   * particular orientation
   * 
   * @param forHorizontalLayout
   *          if true, constructs params for a containing LinearLayout with
   *          horizontal orientation: width wraps content, height matches
   *          container's. If false, width matches container's, height wraps
   *          content
   */
  public static LinearLayout.LayoutParams layoutParams(
      boolean forHorizontalLayout) {
    LinearLayout.LayoutParams params;
    if (forHorizontalLayout)
      params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
          LayoutParams.MATCH_PARENT);
    else
      params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
          LayoutParams.WRAP_CONTENT);
    return params;
  }

  /**
   * Construct LayoutParams for child views of a LinearLayout container
   * 
   * @param container
   * @return LayoutParams appropriate to the container's orientation
   */
  public static LinearLayout.LayoutParams layoutParams(LinearLayout container) {
    return layoutParams(container.getOrientation() == LinearLayout.HORIZONTAL);
  }

  /**
   * A subclass of View that doesn't try to take over its container if
   * LayoutParams.WRAP_CONTENT is given (see issue #5)
   */
  public static class OurBaseView extends View {

    public OurBaseView(Context context) {
      super(context);
      UITools.applyDebugColors(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      // Get the width measurement
      int widthSize = getMeasurement(widthMeasureSpec, 0);
      // Get the height measurement
      int heightSize = getMeasurement(heightMeasureSpec, 0);
      // MUST call this to store the measurements
      setMeasuredDimension(widthSize, heightSize);
    }

    /**
     * Utility to return a view's standard measurement. Uses the supplied size
     * when constraints are given. Attempts to hold to the desired size unless
     * it conflicts with provided constraints.
     * 
     * @param measureSpec
     *          Constraints imposed by the parent
     * @param contentSize
     *          Desired size for the view
     * @return The size the view should be.
     */
    public static int getMeasurement(int measureSpec, int contentSize) {
      int specMode = View.MeasureSpec.getMode(measureSpec);
      int specSize = View.MeasureSpec.getSize(measureSpec);
      int resultSize = 0;
      switch (specMode) {
      case View.MeasureSpec.UNSPECIFIED:
        // Big as we want to be
        resultSize = contentSize;
        break;
      case View.MeasureSpec.AT_MOST:
        // Big as we want to be, up to the spec
        resultSize = Math.min(contentSize, specSize);
        break;
      case View.MeasureSpec.EXACTLY:
        // Must be the spec size
        resultSize = specSize;
        break;
      }

      return resultSize;
    }

  }

  /**
   * Get (brief) information about a MotionEvent
   * 
   * This is similar to MotionEvent.actionToString(...), but that method is not
   * supported for older API levels
   * 
   */
  public static String dump(MotionEvent event) {
    if (event == null)
      return d(event);

    int action = event.getActionMasked();
    int index = event.getActionIndex();
    StringBuilder sb = new StringBuilder("ACTION_");
    switch (action) {
    default:
      sb.append("***UNKNOWN:" + action + "***");
      break;
    case MotionEvent.ACTION_CANCEL:
      sb.append("CANCEL");
      break;
    case MotionEvent.ACTION_DOWN:
      sb.append("DOWN");
      break;
    case MotionEvent.ACTION_UP:
      sb.append("UP");
      break;
    case MotionEvent.ACTION_MOVE:
      sb.append("MOVE");
      break;
    case MotionEvent.ACTION_POINTER_DOWN:
      sb.append("DOWN(" + index + ")");
      break;
    case MotionEvent.ACTION_POINTER_UP:
      sb.append("UP(" + index + ")");
      break;
    }
    return sb.toString();
  }

  private static int sDebugColorIndex;

}
