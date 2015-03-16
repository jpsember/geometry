package com.js.basic;

import java.util.List;

public class Rect {

  public float midX() {
    return (x + width * .5f);
  }

  public float midY() {
    return (y + height * .5f);
  }

  public float maxDim() {
    return Math.max(width, height);
  }

  public float minDim() {
    return Math.min(width, height);
  }

  public boolean equals(Rect r) {
    return r != null && r.x == x && r.y == y && r.width == width
        && r.height == height;
  }

  public Rect(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.width = w;
    this.height = h;
  }

  public void setTo(Rect r) {
    setTo(r.x, r.y, r.width, r.height);
  }

  public void setTo(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.width = w;
    this.height = h;
  }

  public String toString(boolean digitsOnly) {
    StringBuilder sb = new StringBuilder();
    Point loc = new Point(x, y);
    Point size = new Point(width, height);
    if (!digitsOnly)
      sb.append("(pos=");
    sb.append(loc);
    if (!digitsOnly)
      sb.append(" size=");
    sb.append(size);
    if (!digitsOnly)
      sb.append(")");
    return sb.toString();
  }

  public String toString() {
    return toString(false);
  }

  public Rect() {
  }

  /**
   * Construct smallest rectangle containing two points
   * 
   * @param pt1
   * @param pt2
   */
  public Rect(Point pt1, Point pt2) {
    x = Math.min(pt1.x, pt2.x);
    y = Math.min(pt1.y, pt2.y);
    width = Math.max(pt1.x, pt2.x) - x;
    height = Math.max(pt1.y, pt2.y) - y;
  }

  public Rect(Rect r) {
    this(r.x, r.y, r.width, r.height);
  }

  public Rect(android.graphics.Rect source) {
    setTo(source);
  }

  public void setTo(android.graphics.Rect source) {
    setTo(source.left, source.bottom, source.width(), source.height());
  }

  public Point bottomRight() {
    return new Point(endX(), y);
  }

  public void inset(float dx, float dy) {
    x += dx;
    y += dy;
    width -= 2 * dx;
    height -= 2 * dy;
  }

  public Point topLeft() {
    return new Point(x, endY());
  }

  public Point bottomLeft() {
    return new Point(x, y);
  }

  public Point topRight() {
    return new Point(endX(), endY());
  }

  public float endX() {
    return x + width;
  }

  public float endY() {
    return y + height;
  }

  public boolean contains(Rect r) {
    return x <= r.x && y <= r.y && endX() >= r.endX() && endY() >= r.endY();
  }

  public void include(Rect r) {
    include(r.topLeft());
    include(r.bottomRight());
  }

  public void include(Point pt) {
    float ex = endX(), ey = endY();
    x = Math.min(x, pt.x);
    y = Math.min(y, pt.y);
    ex = Math.max(ex, pt.x);
    ey = Math.max(ey, pt.y);
    width = ex - x;
    height = ey - y;
  }

  public float distanceFrom(Point pt) {
    return MyMath.distanceBetween(pt, nearestPointTo(pt));
  }

  /**
   * Find the nearest point within the rectangle to a query point
   * 
   * @param queryPoint
   */
  public Point nearestPointTo(Point queryPoint) {
    return new Point(MyMath.clamp(queryPoint.x, x, endX()), MyMath.clamp(
        queryPoint.y, y, endY()));
  }

  public void translate(float dx, float dy) {
    x += dx;
    y += dy;
  }

  public Point midPoint() {
    return new Point(midX(), midY());
  }

  public boolean contains(Point pt) {
    return x <= pt.x && y <= pt.y && endX() >= pt.x && endY() >= pt.y;
  }

  public void translate(Point tr) {
    translate(tr.x, tr.y);
  }

  /**
   * Scale x,y,width,height by factor
   * 
   * @param f
   */
  public void scale(float f) {
    x *= f;
    y *= f;
    width *= f;
    height *= f;
  }

  public void snapToGrid(float gridSize) {
    float x2 = endX();
    float y2 = endY();
    x = MyMath.snapToGrid(x, gridSize);
    y = MyMath.snapToGrid(y, gridSize);
    width = MyMath.snapToGrid(x2, gridSize) - x;
    height = MyMath.snapToGrid(y2, gridSize) - y;
  }

  /**
   * Get point for corner of rectangle
   * 
   * @param i
   *          corner number (0..3), bottomleft ccw to topleft
   * @return corner
   */
  public Point corner(int i) {
    Point ret = null;

    switch (i) {
    default:
      throw new IllegalArgumentException();
    case 0:
      ret = bottomLeft();
      break;
    case 1:
      ret = bottomRight();
      break;
    case 2:
      ret = topRight();
      break;
    case 3:
      ret = topLeft();
      break;
    }

    return ret;
  }

  public static Rect rectContainingPoints(List<Point> a) {
    if (a.isEmpty())
      throw new IllegalArgumentException();
    Rect r = null;
    for (Point pt : a) {
      if (r == null)
        r = new Rect(pt, pt);
      else
        r.include(pt);
    }
    return r;
  }

  public static Rect rectContainingPoints(Point s1, Point s2) {
    Point m1 = new Point(Math.min(s1.x, s2.x), Math.min(s1.y, s2.y));
    Point m2 = new Point(Math.max(s1.x, s2.x), Math.max(s1.y, s2.y));
    return new Rect(m1.x, m1.y, m2.x - m1.x, m2.y - m1.y);
  }

  public boolean intersects(Rect t) {
    return (x < t.endX() && endX() > t.x && y < t.endY() && endY() > t.y);
  }

  public Point size() {
    return new Point(width, height);
  }

  public float x, y, width, height;

}