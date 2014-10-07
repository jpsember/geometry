package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.js.geometry.MyMath;
import com.js.geometry.Point;

public class EdSegment extends EdObject {

	public EdSegment() {
	}

	public EdSegment(float[] p1) {
		for (int i = 0; i < p1.length; i += 2)
			setPoint(i / 2, new Point(p1[i + 0], p1[i + 1]));
	}

	public EdSegment(Point p1, Point p2) {
		setPoint(0, p1);
		setPoint(1, p2);
	}

	public EdSegment(float x0, float y0, float x1, float y1) {
		this(new Point(x0, y0), new Point(x1, y1));
	}

	public boolean complete() {
		return nPoints() >= 2;
	}

	public float distFrom(Point pt) {
		Point p1 = getPoint(0);
		Point p2 = getPoint(1);
		return MyMath.ptDistanceToSegment(pt, p1, p2, null);
	}

	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory() {
		public EdObject construct() {
			return new EdSegment();
		}

		public String getTag() {
			return "seg";
		}

		public EdObject parse(Map map, int flags) {
			EdSegment seg = new EdSegment();
			seg.setFlags(flags);
			ArrayList<Float> f = (ArrayList<Float>) map.get("points");
			for (int i = 0; i < 2; i++) {
				Point pt = new Point(f.get(i * 2), f.get(i * 2 + 1));
				seg.addPoint(pt);
			}
			return seg;
		}

		public Map write(EdObject obj) {
			Map<String, Object> map = new HashMap();
			map.put("type", getTag());
			EdSegment d = (EdSegment) obj;
			ArrayList<Float> f = new ArrayList();
			for (int i = 0; i < 2; i++) {
				Point pt = d.getPoint(i);
				f.add(pt.x);
				f.add(pt.y);
			}
			map.put("points", f);
			return map;
		}

		public String getMenuLabel() {
			return "Add segment";
		}

	};

	// public void render(Color color, int stroke, int markType) {
	// V.pushColor(color, isActive() ? Color.BLUE : Color.GRAY);
	// // V.pushColor(color, SEGMENT_COLOR);
	// V.pushStroke(stroke);
	//
	// Point prev = null;
	// for (int i = 0; i < nPoints(); i++) {
	// Point pt = getPoint(i);
	// if (prev != null)
	// V.drawLine(prev, pt);
	// if (markType >= 0)
	// V.mark(pt, markType);
	// prev = pt;
	// }
	// V.pop();
	// if (complete()) {
	// Point p0 = getPoint(0);
	// Point p1 = getPoint(1);
	// float theta = MyMath.polarAngle(p0, p1);
	// if (Editor.withLabels(true)) {
	// V.pushScale(.6);
	// Point offset = MyMath.ptOnCircle(zero, theta,
	// V.getScale() * 1.5);
	// V.draw("0", Point.add(p0, offset, null), TX_FRAME | TX_BGND);
	// V.draw("1", Point.add(p1, offset, null), TX_FRAME | TX_BGND);
	// V.pop();
	// }
	// if (Editor.withLabels(false)) {
	// Point cp = Point.midPoint(p0, p1);
	// plotLabel(MyMath.ptOnCircle(cp, theta - Math.PI / 2,
	// V.getScale()));
	// }
	// }
	// V.pop();
	// }

	// /**
	// * Construct and show a segment from a pair of endpoints
	// *
	// * @param x0
	// * @param y0
	// * @param x1
	// * @param y1
	// * @param c
	// * color to display with, or null for default
	// * @param stroke
	// * if >= 0, stroke to use; else, default
	// * @return empty string
	// */
	// public static String show(float x0, float y0, float x1, float y1,
	// Color c, int stroke) {
	// return show(x0, y0, x1, y1, c, stroke, 0);
	// }
	//
	// private static String show(float x0, float y0, float x1, float y1,
	// Color c, int stroke, int arrowFlags) {
	// return T.show(new MiscLine(x0, y0, x1, y1, c, stroke, arrowFlags));
	// }
	//
	// public static String showDirected(Point p0, Point p1, Color c,
	// int stroke) {
	// if (p0 == null || p1 == null)
	// return "";
	// return show(p0.x, p0.y, p1.x, p1.y, c, stroke, (1 << 0));
	// }
	//
	// public static String showDirected(Point p0, Point p1) {
	// return showDirected(p0, p1, null, -1);
	// }
	//
	// public static String show(Point p0, Point p1, Color c, int stroke) {
	// return show(p0.x, p0.y, p1.x, p1.y, c, stroke);
	// }
	//
	// public static String show(Point p0, Point p1, Color c) {
	// return show(p0.x, p0.y, p1.x, p1.y, c, -1);
	// }
	//
	// public static String show(Point p0, Point p1) {
	// return show(p0, p1, Color.red, -1);
	// }

	// private static class MiscLine implements Renderable {
	// public MiscLine(float x0, float y0, float x1, float y1, Color c,
	// int stroke, int arrowFlags) {
	// this.pt0 = new Point(x0, y0);
	// this.pt1 = new Point(x1, y1);
	// if (c == null)
	// c = Color.red;
	// if (stroke < 0)
	// stroke = V.STRK_NORMAL;
	//
	// this.c = c;
	// this.stroke = stroke;
	// this.arrowFlags = arrowFlags;
	// }
	//
	// private int arrowFlags;
	// private Point pt0, pt1;
	// private Color c;
	// private int stroke;
	//
	// public void render(Color c, int stroke, int markType) {
	// if (stroke < 0)
	// stroke = this.stroke;
	// V.pushStroke(stroke);
	// if (c == null)
	// c = this.c;
	// V.pushColor(c);
	// V.drawLine(pt0, pt1);
	// for (int i = 0; i < 2; i++) {
	// Point pt = i == 1 ? pt0 : pt1;
	// Point pt2 = (i == 1) ? pt1 : pt0;
	//
	// if ((arrowFlags & (1 << i)) != 0) {
	// plotArrowHead(pt, MyMath.polarAngle(pt2, pt));
	// } else {
	// if (markType >= 0) {
	// V.mark(pt, markType);
	// }
	// }
	// //
	// //
	// // if (markType >= 0) {
	// // V.mark(pt0, markType);
	// // V.mark(pt1, markType);
	// }
	// V.popColor();
	// V.popStroke();
	// }
	// };

	// public static void plotDirectedLine(Point p0, Point p1) {
	// plotDirectedLine(p0, p1, false, true);
	// }
	//
	// public static void plotDirectedLine(Point p0, Point p1, boolean p0Head,
	// boolean p1Head) {
	// V.drawLine(p0, p1);
	// float len = p0.distance(p1);
	// // draw arrowheads
	// if (len > 0) {
	// float theta = MyMath.polarAngle(p0, p1);
	//
	// for (int h = 0; h < 2; h++) {
	// Point ep = h == 0 ? p0 : p1;
	// if ((h == 0 ? p0Head : p1Head)) {
	// plotArrowHead(ep, theta);
	// }
	// }
	// }
	// }
	//
	// public static void plotArrowHead(Point pt, float theta) {
	// final float AH_LEN = 1.2;
	// final float AH_ANG = Math.PI * .85;
	// float th = theta;
	//
	// Point a0 = MyMath.ptOnCircle(pt, th + AH_ANG, AH_LEN);
	// V.drawLine(pt, a0);
	// Point a1 = MyMath.ptOnCircle(pt, th - AH_ANG, AH_LEN);
	// V.drawLine(pt, a1);
	// }
	//
	// public void renderTo(Graphics2D g) {
	// Line2D.float wl = new Line2D.float();
	// wl.setLine(getPoint(0), getPoint(1));
	// g.draw(wl);
	// }

}