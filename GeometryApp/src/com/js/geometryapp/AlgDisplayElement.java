package com.js.geometryapp;

import android.content.Context;
import android.graphics.Color;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;

import static com.js.basic.Tools.*;

public abstract class AlgDisplayElement {

	public AlgDisplayElement() {
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
		sProgram.render(sPolyline, null);
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
		extendPolyline(p1);
		extendPolyline(p2);
		renderPolyline();
		float angleOfRay = MyMath.polarAngleOfSegment(p1, p2);

		float arrowHeadAngle = MyMath.PI * .8f;
		float arrowHeadLength = 15.0f;
		Point p3 = MyMath.pointOnCircle(p2, angleOfRay - arrowHeadAngle,
				arrowHeadLength);
		Point p4 = MyMath.pointOnCircle(p2, angleOfRay + arrowHeadAngle,
				arrowHeadLength);
		extendPolyline(p3);
		extendPolyline(p2);
		extendPolyline(p4);
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
		renderPoint(point, 3.0f);
	}

	public static void renderPoint(Point point, float radius) {
		setLineWidthState(2.0f);

		extendPolyline(point.x - radius, point.y - radius);
		extendPolyline(point.x + radius, point.y - radius);
		extendPolyline(point.x + radius, point.y + radius);
		extendPolyline(point.x - radius, point.y + radius);
		closePolyline();
		renderPolyline();
	}

	/**
	 * Have the algorithm stepper display elements prepare to use an OpenGL
	 * renderer; i.e. setting up shaders and fonts. Should be called within
	 * onSurfaceCreated(...)
	 */
	public static void setRenderer(AlgorithmRenderer renderer) {
		OurGLTools.ensureRenderThread();
		Context sContext = renderer.context();
		sVertexShader = GLShader.readVertexShader(sContext,
				R.raw.simple_vertex_shader);
		sFragmentShader = GLShader.readFragmentShader(sContext,
				R.raw.simple_fragment_shader);
		sProgram = new GLProgram(renderer, sVertexShader, sFragmentShader);
		sProgram.setTransformName(AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		sFont = new Font(24);
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

	private int mColor;
	private float mLineWidth;

	private static Font sFont;
	private static GLShader sVertexShader;
	private static GLShader sFragmentShader;
	private static GLProgram sProgram;
	private static Polyline sPolyline;
	private static float sLineWidth;
	private static int sColor;
}
