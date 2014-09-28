package com.js.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.StringTokenizer;

import com.js.basic.Tools;

import static com.js.basic.Tools.*;
import static com.js.geometry.MyMath.*;

import android.graphics.Matrix;

public class Polygon {
	public static final int TESTPOLY_SQUARE = 0;
	public static final int TESTPOLY_DIAMOND = 1;
	public static final int TESTPOLY_LARGE_RECTANGLE = 2;
	public static final int TESTPOLY_TRIANGLE_HORZ_BASE = 3;
	public static final int TESTPOLY_TRIANGLE_SLOPED_BASE = 4;
	public static final int TESTPOLY_CONCAVE_BLOB = 5;
	public static final int TESTPOLY_Y_EQUALS_X_SQUARED = 6;
	public static final int TESTPOLY_MERGESPLIT = 7;
	public static final int TESTPOLY_VULCAN_SYMBOL = 8;
	public static final int TESTPOLY_NONSIMPLE_SPIRAL = 9;
	public static final int TESTPOLY_NONSIMPLE_BOX = 10;
	public static final int TESTPOLY_LARGE_CONVEX = 11;
	public static final int TESTPOLY_QUADRATIC_FILTER = 12;
	public static final int TESTPOLY_SPIROGRAPH = 13;
	public static final int TESTPOLY_SPIROGRAPH2 = 14;

	public static final int TESTPOLY_DRAGON_X = 1000;

	public int numVertices() {
		return mVertices.size();
	}

	public Polygon() {
	}

	public Polygon(Polygon polygon) {
		for (int i = 0; i < polygon.numVertices(); i++)
			add(polygon.vertex(i));
	}

	public Polygon(Collection<Point> points) {
		for (Point point : points)
			add(point);
	}

	private static ArrayList<String> extractTokens(String s) {
		ArrayList<String> list = new ArrayList();
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		return list;
	}

	public Polygon(String script, Point translate, float scale) {
		float x = -99;
		for (String s : extractTokens(script)) {
			if (s.length() == 0)
				continue;

			float y = Float.parseFloat(s);
			if (x == -99) {
				x = y;
			} else {
				x = x * scale + translate.x;
				y = y * scale + translate.y;
				Point pt = new Point(x, y);
				mVertices.add(pt);
				x = -99;
			}
		}
	}

	public Polygon(String script) {
		this(script, new Point(), 1);
	}

	private static final String testPolygonScripts[] = {
			// A square centered at 0,0
			"-150 -150 150 -150 150 150 -150 150",
			// An elongated diamond centered at 0,0
			"0 -200 210 10 30 210 -200 0",
			// a large approx axis-aligned rectangle
			"40 40 600 60 650 400 80 370",
			// a large triangle with horizontal base
			"0 0 800 0 400 580",
			// a large triangle with sloped base
			"0 0 800 45 400 580",//
			// a concave blob
			"28.124 97.673 91.280 52.390 156.821 65.499 166.354 126.272 190.186 213.262 247.385 262.119 327.225 252.586 389.189 200.153 408.256 103.630 444.004 42.857 538.144 47.624 581.044 120.314 534.568 281.184 442.814 374.133 292.667 411.074 166.354 409.881 59.106 339.576 23.357 288.334 9.058 172.745",
			//
			// X-coordinates equal vertex numbers, y coordinates are numbers
			// squared
			"0 0 1 1 2 4 3 9 4 16 5 25 6 36 7 49",

			// A polygon with merge & split vertices to test triangulation
			"30 13  46 49  59   22    89 58      72  89       63    70       48   89  15 39",

			// A sort of inverted V
			"50 50  500 500 950 50 500 900",

			// A nonsimple spiral
			"30 45  43 26  89 14 121 36 130 61 119 77  91 87  47 84  21 68  19 43  28 34 "
					+ "60 27  92 25 107 35 119 56 111 70  84 77  46 75  33 64  33 49  47 37  78 31  98 39 102 52  99 62  80 68  51 71  27 57",

			// A shape with nonsimple corners and a couple of twists:
			"38 38 64 30 98 34 110 39 122 36 117 29 108 28 103 41 101 57 105 72 113 79 122 77 122 68 114 65 98 65 "
					+ "80 68 60 71 40 66 23 71 24 80 32 86 42 84 46 76 44 61 49 51 61 49 74 51 74 59 65 62 56 58 50 48 42 45 "
					+ "34 48 27 52 21 51 20 43 21 30 31 25 40 28 42 30 51 42 60 42 64 37 58 35 36 42 31 44 28 41 28 36 32 35",

			// A largish convex polygon
			"36  21    59  15    80  13    94  14   102  17   106  23   109  33   111  46   107  68    95  82    67  91    40  91    23  84    11  70    16  35", };

	public static Polygon testPolygon(int index) {
		Random random = new Random(1);
		Polygon poly;
		if (index >= TESTPOLY_DRAGON_X && index < TESTPOLY_DRAGON_X + 15) {
			poly = dragonPolygon(index - TESTPOLY_DRAGON_X);
		} else if (index == TESTPOLY_QUADRATIC_FILTER) {
			poly = polygon();
			int nv = 400;

			float x, y, y2;
			y = 0;
			y2 = 0;

			for (int i = 0; i < nv; i += 2) {
				x = i * (900.0f / nv) + 50;
				y = 50;
				y2 = ((x - 30) / 920.0f) * 100 + y;
				if (i < (nv * 7) / 8) {
					y2 = y;
					x += .01f;
				}
				poly.add(x, y);
				poly.add(x, y2);
			}
			poly.add(450, (y + y2) * .5f);
		} else if (index == TESTPOLY_SPIROGRAPH
				|| index == TESTPOLY_SPIROGRAPH2) {

			float sf = (index == TESTPOLY_SPIROGRAPH2) ? 3 : 1;

			poly = polygon();
			// [JSPolygon polygon];

			float f1 = 19 / sf, f2 = 13 / sf, f3 = 17 / sf;

			for (int i = 0; i < 250 / sf; i++) {
				float scale = 120 * MyMath.cos(i / f1) + 300;
				Point pt = new Point(MyMath.sin(i / f2) * scale, MyMath.cos(i
						/ f3)
						* scale);
				if (index == TESTPOLY_SPIROGRAPH2) {
					float drift = 20;
					pt.x += random.nextFloat() * drift;
					pt.y += random.nextFloat() * drift;
				}
				poly.add(pt);
			}
		} else {
			poly = polygonWithScript(testPolygonScripts[index]);
		}

		int pad = 80;
		poly.transformToFitRect(new Rect(pad, pad, 1400 - pad * 2,
				1000 - pad * 2), true);
		return poly;
	}

	public static Polygon discPolygon(Point origin, float radius,
			int numberOfSides) {
		Polygon polygon = new Polygon();

		for (int i = 0; i < numberOfSides; i++) {
			Point v = MyMath.pointOnCircle(new Point(300, 300), MyMath.M_DEG
					* i * (360.0f / numberOfSides), 100);
			polygon.add(v);
		}
		return polygon;
	}

	private static Polygon dragonPolygon(int depth) {

		String startScript = "200 500 800 510 700 705 880 780 890 250 300 260 310 600 180 610 180 500";
		if (depth < 6)
			startScript = "200 500 800 510 700 705 880 780 890 250 300 260";
		Polygon p = polygonWithScript(startScript);

		int extraVerts = p.numVertices() - 2;
		Tools.ASSERT(p.numVertices() >= 3);

		while (depth > 0) {
			depth--;
			int nEdges = p.numVertices() - extraVerts - 1;
			int cursor = 0;
			boolean parity = false;
			while (nEdges > 0) {
				nEdges--;
				Point a = p.vertex(cursor);
				Point b = p.vertex(cursor + 1);

				float length = MyMath.distanceBetween(a, b);

				float factor = .45f; // .48;
				float parm1 = .5f;
				float parm2 = .5f;

				Point c;
				if (parity) {
					c = MyMath.pointBesideSegment(a, b, length * factor, parm1);
				} else {
					c = MyMath.pointBesideSegment(b, a, length * factor, parm2);
				}
				// JSVertex *vc = [context allocVertex:c];
				p.add(c, cursor + 1);
				cursor += 2;
				parity ^= true;
			}
		}
		p.reverse();
		p.rotateBy(MyMath.M_DEG);
		p.perturb(new Random(1));
		return p;
	}

	public static Polygon polygon() {
		return new Polygon();
	}

	public static Polygon polygonWithScript(String script) {
		return new Polygon(script);
	}

	public Point vertex(int index) {
		return mVertices.get(index);
	}

	private int modSize(int index) {
		return MyMath.myMod(index, numVertices());
	}

	// Get vertex; index is taken modulo the current size
	public Point vertexMod(int index) {
		return mVertices.get(modSize(index));
	}

	/**
	 * Embed the polygon into its context's mesh; returns index of first vertex
	 * within mesh
	 */
	public int embed(GeometryContext context) {
		return embed(context, 0, 0);
	}

	public int embed(GeometryContext context, int vertexFlags, int edgeFlags) {
		int baseVertex = embedVertices(context, vertexFlags);
		Vertex prevVertex = context.vertex(baseVertex + numVertices() - 1);
		for (int i = 0; i < numVertices(); i++) {
			Vertex currentVertex = context.vertex(baseVertex + i);
			Edge edge = context.addEdge(prevVertex, currentVertex);
			edge.addFlags(Edge.FLAG_POLYGON | edgeFlags);
			edge.dual().addFlags(edgeFlags);

			prevVertex = currentVertex;
		}
		return baseVertex;
	}

	public int embedVertices(GeometryContext context, int vertexFlags) {

		int embeddedVertexIndex = context.vertexBuffer().size();

		for (int i = 0; i < numVertices(); i++) {
			Point pt = vertex(i);
			Vertex v = context.addVertex(pt);
			v.addFlags(vertexFlags);
		}
		return embeddedVertexIndex;
	}

	public Rect bounds() {
		if (numVertices() < 1)
			die("too few vertices");
		Point p0 = vertex(0);
		Float x0 = p0.x, x1 = p0.x, y0 = p0.y, y1 = p0.y;
		for (int i = 1; i < numVertices(); i++) {
			Point pt = vertex(i);
			x0 = Math.min(x0, pt.x);
			x1 = Math.max(x1, pt.x);
			y0 = Math.min(y0, pt.y);
			y1 = Math.max(y1, pt.y);
		}
		return new Rect(x0, y0, x1 - x0, y1 - y0);
	}

	public float area() {
		float a = 0;
		for (int i = 0; i < numVertices(); i++) {
			Point p1 = vertex(i);
			Point p2 = vertexMod(i + 1);
			a += p1.x * p2.y - p2.x * p1.y;
		}
		a *= .5f;
		return a;
	}

	public float boundaryLength() {
		float a = 0;
		for (int i = 0; i < numVertices(); i++) {
			Point p1 = vertex(i);
			Point p2 = vertexMod(i + 1);
			a += MyMath.distanceBetween(p1, p2);
		}
		return a;
	}

	/**
	 * Determine if this polygon is convex. Undefined result if polygon isn't
	 * simple, or doesn't have a ccw winding
	 */
	public boolean isConvex() {
		int j = numVertices();
		if (j < 3)
			return false;

		Point vPrev = vertex(j - 2);
		Point vNext = vertex(j - 1);
		float dx2 = vNext.x - vPrev.x;
		float dy2 = vNext.y - vPrev.y;

		for (int i = 0; i < j; i++) {
			vPrev = vNext;
			vNext = vertex(i);

			float dx1 = dx2;
			float dy1 = dy2;
			dx2 = vNext.x - vPrev.x;
			dy2 = vNext.y - vPrev.y;
			float crossProduct = dx1 * dy2 - dy1 * dx2;
			if (crossProduct <= 0)
				return false;
		}
		return true;
	}

	/**
	 * Determine if polygon is CCW or CW oriented
	 * 
	 * @return 1 if CCW, -1 if CW, 0 if unknown (e.g. nonsimple)
	 */
	public int orientation() {
		if (numVertices() < 3)
			die("too few vertices");
		float totalSwept = 0;
		Point v0 = vertexMod(-2);
		Point v1 = vertexMod(-1);
		float angle01 = pseudoPolarAngleOfSegment(v0, v1);

		for (int i = 0; i < numVertices(); i++) {
			Point v2 = vertex(i);
			float angle12 = pseudoPolarAngleOfSegment(v1, v2);
			float subtendedAngle = normalizePseudoAngle(angle12
					- angle01);
			totalSwept += subtendedAngle;
			angle01 = angle12;
			v1 = v2;
		}

		if (Math.abs(totalSwept - (-MyMath.PSEUDO_ANGLE_RANGE)) < .01f) {
			return -1;
		}
		if (!(Math.abs(totalSwept - MyMath.PSEUDO_ANGLE_RANGE) < .01f)) {
			return 0;
		}
		return 1;
	}

	// Returns true iff polygon has ccw orientation
	public boolean isCCW(GeometryContext context) {
		int orientation = orientation();
		if (orientation == 0)
			GeometryException.raise("Polygon winding number unknown");
		return (orientation == 1);
	}

	public boolean neighbors(int i1, int i2) {
		int diff = Math.abs(i1 - i2);
		return diff == 1 || diff == numVertices() - 1;
	}

	public static Polygon circleWithOrigin(Point origin, float radius,
			int numVertices) {
		Polygon p = polygon();
		for (int i = 0; i < numVertices; i++) {
			Point pt = MyMath.pointOnCircle(origin, (i * MyMath.PI * 2)
					/ numVertices, radius);
			p.add(pt);
		}
		return p;

	}

	// These methods mutate the polygon:
	public void perturb(Random random) {
		for (Point pt : mVertices) {
			MyMath.perturb(random, pt);
		}
	}

	// Insert vertex
	public void add(Point vertex, int index) {
		mVertices.add(index, vertex);
	}

	// Insert vertex at end of existing ones
	public void add(Point vertex) {
		mVertices.add(vertex);
	}

	public void add(float x, float y) {
		add(new Point(x, y));
	}

	// Replace vertex
	public void setVertex(Point vertex, int index) {
		throw new UnsupportedOperationException();
	}

	public void removeVerticesInRange(int start, int length) {
		throw new UnsupportedOperationException();
	}

	public void removeVertex(int index) {
		throw new UnsupportedOperationException();
	}

	public void reverse() {
		ArrayList<Point> list = new ArrayList();
		for (int i = numVertices() - 1; i >= 0; i--) {
			list.add(vertex(i));
		}
		mVertices = list;
	}

	public void rotateBy(float angleInRadians) {
		Matrix m = new Matrix();
		m.setRotate(angleInRadians / MyMath.M_DEG);
		apply(m);
	}

	public void transformToFitRect(Rect rect, boolean preserveAspectRatio) {
		Rect bounds = bounds();
		Matrix t = MyMath.calcRectFitRectTransform(bounds, rect,
				preserveAspectRatio);
		apply(t);
	}

	public void apply(Matrix transform) {
		for (int i = 0; i < numVertices(); i++) {
			Point v = vertex(i);
			v.apply(transform);
		}
	}

	public void clear() {
		mVertices.clear();
	}

	@Override
	public String toString() {
		String prefix = "Polygon: ";
		StringBuilder sb = new StringBuilder(prefix);
		int displayed = 0;
		for (Point pt : mVertices) {
			if (displayed == 6) {
				displayed = 0;
				sb.append("\n" + sp(prefix.length()));
			}
			sb.append("    ");
			sb.append(f((int) pt.x, 4));
			sb.append(' ');
			sb.append(f((int) pt.y, 4));
			displayed++;
		}
		return sb.toString();
	}

	private ArrayList<Point> mVertices = new ArrayList();

}
