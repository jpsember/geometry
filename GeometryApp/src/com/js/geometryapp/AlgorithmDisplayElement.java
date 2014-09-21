package com.js.geometryapp;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.android.MyActivity;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;

import static com.js.basic.Tools.*;

public abstract class AlgorithmDisplayElement {

	public AlgorithmDisplayElement() {
		doNothing();
		mLineWidth = sLineWidth;
		mColor = sColor;
	}

	public abstract void render();

	public static void startPolyline(Point point) {
		sPolyline = new Polyline();
		sPolyline.setColor(sColor);
		sPolyline.setLineWidth(sLineWidth);
		extendPolyline(point);
	}

	public static void extendPolyline(Point point) {
		if (sPolyline == null)
			startPolyline(point);
		sPolyline.add(point);
	}

	public static void extendPolyline(float x, float y) {
		extendPolyline(new Point(x, y));
	}

	public static void closePolyline() {
		sPolyline.close();
	}

	public static void renderPolyline() {
		sPolyline.render();
		sPolyline = null;
	}

	public static void renderLine(float x1, float y1, float x2, float y2) {
		extendPolyline(x1, y1);
		extendPolyline(x2, y2);
		renderPolyline();
	}

	public static void renderLine(Point p1, Point p2) {
		renderLine(p1.x, p1.y, p2.x, p2.y);
	}

	public static void renderRay(Point p1, Point p2) {
		float angleOfRay = MyMath.polarAngleOfSegment(p1, p2);
		float length = MyMath.distanceBetween(p1, p2);
		float setback = sArrowheadLength * .3f;
		boolean drawArrowhead = (length > 2 * setback);
		Point p2b = p2;
		if (drawArrowhead) {
			p2b = MyMath
					.interpolateBetween(p1, p2, (length - setback) / length);
			Matrix m = buildRotationMatrix(angleOfRay);
			m.postConcat(buildTranslationMatrix(p2));
			sPolygonProgram.setColor(sColor);
			sPolygonProgram.render(sArrowheadMesh, null, m);
		}
		extendPolyline(p1);
		extendPolyline(p2b);
		renderPolyline();
	}

	/**
	 * Set the line width state (as opposed to an instance's line width)
	 * 
	 * @param lineWidth
	 */
	public static void setLineWidthState(float lineWidth) {
		sLineWidth = lineWidth;
	}

	public static void setColorState(int color) {
		sColor = color;
	}

	public static void renderPoint(Point point) {
		renderPoint(point, 1);
	}

	public static void renderPoint(Point point, float radius) {
		Matrix matrix = null;
		Point translation = null;
		// If no scaling required, we can just use a translation vector
		if (radius == 1) {
			translation = point;
		} else {
			Matrix translationMatrix = buildTranslationMatrix(point);
			matrix = buildScaleMatrix(radius);
			matrix.postConcat(translationMatrix);
		}
		sPolygonProgram.setColor(sColor);
		sPolygonProgram.render(sPointMesh, translation, matrix);
	}

	/**
	 * Have the algorithm stepper display elements prepare to use an OpenGL
	 * renderer; i.e. setting up shaders and fonts. Should be called within
	 * onSurfaceCreated(...)
	 */
	public static void setRenderer(AlgorithmRenderer renderer) {
		OurGLTools.ensureRenderThread();
		Polyline.prepareRenderer(renderer,
				AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		sPolygonProgram = new PolygonProgram(renderer,
				AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		buildArrowheads(renderer);
		buildPoints(renderer);
		sFont = new Font((int) (18 * MyActivity.density()));
		sLineWidth = 1.0f;
		sColor = Color.WHITE;
	}

	protected static void renderFrameTitle(String sFrameTitle) {
		Point p = new Point(10, 10 + sFont.lineHeight());

		sFont.setColor(Color.BLACK);
		sFont.render(sFrameTitle, p);
	}

	public void setLineWidth(float lineWidth) {
		mLineWidth = lineWidth;
	}

	public float lineWidth() {
		return mLineWidth;
	}

	public int color() {
		return mColor;
	}

	public void setColor(int color) {
		mColor = color;
	}

	private static Matrix buildScaleMatrix(float scale) {
		Matrix m = new Matrix();
		m.setScale(scale, scale);
		return m;
	}

	private static Matrix buildTranslationMatrix(Point p) {
		Matrix m = new Matrix();
		m.setTranslate(p.x, p.y);
		return m;
	}

	private static Matrix buildRotationMatrix(float radians) {
		Matrix m = new Matrix();
		m.setRotate(radians / MyMath.M_DEG);
		return m;
	}

	private static void buildArrowheads(AlgorithmRenderer renderer) {
		sArrowheadLength = AlgorithmRenderer.algorithmToDensityPixels() * 18;

		Polygon p = Polygon.polygonWithScript("0 0 -1 .4 -1 -.4");
		p.apply(buildScaleMatrix(sArrowheadLength));
		sArrowheadMesh = PolygonMesh.meshForConvexPolygon(p);
	}

	private static void buildPoints(AlgorithmRenderer renderer) {
		int POINT_VERTICES = 10;
		Polygon p = Polygon.circleWithOrigin(Point.ZERO,
				4.0f * AlgorithmRenderer.algorithmToDensityPixels(),
				POINT_VERTICES);
		sPointMesh = PolygonMesh.meshForConvexPolygon(p);
	}

	static PolygonProgram polygonProgram() {
		return sPolygonProgram;
	}

	private int mColor;
	private float mLineWidth;

	private static float sArrowheadLength;
	private static PolygonProgram sPolygonProgram;
	private static PolygonMesh sArrowheadMesh;
	private static PolygonMesh sPointMesh;
	private static Font sFont;
	private static Polyline sPolyline;
	private static float sLineWidth;
	private static int sColor;
}
