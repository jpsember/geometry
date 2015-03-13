package com.js.geometry;

import static com.js.basic.Tools.*;

import com.js.basic.MyMath;
import com.js.android.MyActivity;
import com.js.geometryapp.RenderTools;
import com.js.basic.Point;

public class Disc implements Renderable {

  public Disc(Point origin, float radius) {
    mOrigin = new Point(origin);
    mRadius = radius;
  }

  public Point getOrigin() {
    return mOrigin;
  }

  public float getRadius() {
    return mRadius;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(d(mOrigin.x));
    sb.append(' ');
    sb.append(d(mOrigin.y));
    sb.append(' ');
    sb.append(d(getRadius()));
    return sb.toString();
  }

  @Override
  public void render(AlgorithmStepper s) {
    Point origin = getOrigin();
    float radius = getRadius();
    final float SIDES_FACTOR = .06f;
    final int SIDES_MIN = 4;
    final int SIDES_MAX = 50;

    // Construct a polygonal approximation to this disc, one that
    // is smooth yet doesn't have more edges than are necessary

    // Get physical size of disc, and take its cube root (approx)
    float pickRadius = MyActivity.getResolutionInfo().inchesToPixelsAlgorithm(
        .14f);
    float scaledRadius = (float) Math.pow(radius / pickRadius, .3f);
    // Scale this by a constant to get the number of polygonal edges
    int numberOfSides = (int) (scaledRadius / SIDES_FACTOR);
    int numberOfSidesClamped = MyMath
        .clamp(numberOfSides, SIDES_MIN, SIDES_MAX);

    float angleSep = ((360 * MyMath.M_DEG) / numberOfSidesClamped);
    Polyline p = new Polyline();
    for (int i = 0; i < numberOfSidesClamped; i++) {
      p.add(MyMath.pointOnCircle(origin, i * angleSep, radius));
    }
    p.close();
    p.render(s);
  }

  /**
   * Get a renderable for this disc
   * 
   * @param filled
   *          true to plot a solid disc vs just its boundary
   */
  public Renderable renderable(boolean filled) {
    if (!filled)
      return this;
    return new Rendered(this, true);
  }

  /**
   * Wrapper class for rendering with additional attributes (e.g. filled flag)
   */
  private static class Rendered implements Renderable {
    public Rendered(Disc disc, boolean filled) {
      mDisc = disc;
      mFilled = filled;
    }

    @Override
    public void render(AlgorithmStepper s) {
      if (!mFilled)
        mDisc.render(s);
      else
        RenderTools.renderPoint(mDisc.getOrigin(), mDisc.getRadius());
    }

    private Disc mDisc;
    private boolean mFilled;
  }

  private Point mOrigin;
  private float mRadius;

}
