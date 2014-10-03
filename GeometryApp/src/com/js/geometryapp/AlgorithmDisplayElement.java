package com.js.geometryapp;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.android.MyActivity;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;

public abstract class AlgorithmDisplayElement {

	public AlgorithmDisplayElement() {
		mLineWidth = sLineWidth;
		mColor = sColor;
	}

	public float lineWidth() {
		return mLineWidth;
	}

	public int color() {
		return mColor;
	}

	public abstract void render();

	static int getElementCount() {
		int r = sElementsConstructed;
		sElementsConstructed = 0;
		return r;
	}

	static void startPolyline(Point point) {
		sPolyline = new Polyline();
		sPolyline.setColor(sColor);
		sPolyline.setLineWidth(sLineWidth);
		extendPolyline(point);
	}

	static void extendPolyline(Point point) {
		if (sPolyline == null)
			startPolyline(point);
		sPolyline.add(point);
	}

	static void extendPolyline(float x, float y) {
		extendPolyline(new Point(x, y));
	}

	static void closePolyline() {
		sPolyline.close();
	}

	static void renderPolyline() {
		sPolyline.render();
		sPolyline = null;
	}

	static void renderLine(float x1, float y1, float x2, float y2) {
		extendPolyline(x1, y1);
		extendPolyline(x2, y2);
		renderPolyline();
	}

	static void renderLine(Point p1, Point p2) {
		renderLine(p1.x, p1.y, p2.x, p2.y);
	}

	static void renderRay(Point p1, Point p2) {
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

	static void setLineWidthState(float lineWidth) {
		sLineWidth = lineWidth;
	}

	static void setColorState(int color) {
		sColor = color;
	}

	static void renderPoint(Point point) {
		renderPoint(point, 1);
	}

	static void renderPoint(Point point, float radius) {
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
	static void setRenderer(AlgorithmRenderer renderer) {
		OurGLTools.ensureRenderThread();
		sRenderer = renderer;
		Polyline.prepareRenderer(renderer,
				AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		sPolygonProgram = new PolygonProgram(renderer,
				AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		buildArrowheads();
		buildPoints();
		sTitleFont = new Font((int) (18 * MyActivity.density()));
		sElementFont = new Font((int) (14 * MyActivity.density()));
		resetRenderStateVars();
	}

	static void renderText(Point location, String text) {
		Point p = new Point(location.x, location.y);

		// Convert location from algorithm space to view space
		Matrix matrix = sRenderer
				.getTransform(AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_DEVICE);
		p.apply(matrix);

		Font font = sElementFont;

		// Place text centered vertically, to the right of the given point
		p.y += font.lineHeight() / 2;
		p.x += font.lineHeight() * .67f;

		font.setColor(sColor);
		font.render(text, p);
	}

	static void renderFrameTitle(String sFrameTitle) {
		Point p = new Point(10, 10 + sTitleFont.lineHeight());

		sTitleFont.setColor(Color.BLACK);
		sTitleFont.render(sFrameTitle, p);
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

	private static void buildArrowheads() {
		sArrowheadLength = AlgorithmRenderer.algorithmToDensityPixels() * 18;

		Polygon p = Polygon.polygonWithScript("0 0 -1 .4 -1 -.4");
		p.apply(buildScaleMatrix(sArrowheadLength));
		sArrowheadMesh = PolygonMesh.meshForConvexPolygon(p);
	}

	private static void buildPoints() {
		int POINT_VERTICES = 10;
		Polygon p = Polygon.circleWithOrigin(Point.ZERO,
				4.0f * AlgorithmRenderer.algorithmToDensityPixels(),
				POINT_VERTICES);
		sPointMesh = PolygonMesh.meshForConvexPolygon(p);
	}

	static PolygonProgram polygonProgram() {
		return sPolygonProgram;
	}

	/**
	 * Reset the global render attributes to their default values
	 */
	static void resetRenderStateVars() {
		sLineWidth = 1.0f;
		sColor = Color.BLUE;
	}

	/**
	 * Set rendering state. If false, any render operations will generate
	 * display elements for later rendering; if true, render operations actually
	 * perform the rendering
	 */
	static void setRendering(boolean f) {
		sRendering = f;
	}

	static boolean rendering() {
		return sRendering;
	}

	private int mColor;
	private float mLineWidth;

	private static float sArrowheadLength;
	private static PolygonProgram sPolygonProgram;
	private static PolygonMesh sArrowheadMesh;
	private static PolygonMesh sPointMesh;
	private static Font sTitleFont;
	private static Font sElementFont;
	private static Polyline sPolyline;
	private static float sLineWidth;
	private static int sColor;
	private static boolean sRendering;
	private static int sElementsConstructed;
	private static AlgorithmRenderer sRenderer;
}
