package com.js.geometryapp;

import android.content.Context;

import com.js.geometry.Point;
import com.js.geometry.R;

public abstract class AlgDisplayElement {

	public AlgDisplayElement() {
		mLineWidth = sLineWidth;
	}

	public abstract void render();

	public static void startPolyline(Point point) {
		sPolyline = new Polyline();
		sPolyline.setColor(1.0f, 0.3f, 0.3f);
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
		sProgram.render(sPolyline, sRenderer, null);
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

	/**
	 * Set the line width state (as opposed to an instance's line width)
	 * 
	 * @param lineWidth
	 */
	public static void setLineWidthState(float lineWidth) {
		sLineWidth = lineWidth;
	}

	public static void renderPoint(Point point) {
		setLineWidthState(2.0f);

		int pointSize = 5;
		extendPolyline(point.x - pointSize, point.y - pointSize);
		extendPolyline(point.x + pointSize, point.y - pointSize);
		extendPolyline(point.x + pointSize, point.y + pointSize);
		extendPolyline(point.x - pointSize, point.y + pointSize);
		closePolyline();
		renderPolyline();
	}

	/**
	 * Have the algorithm stepper display elements prepare to use an OpenGL
	 * renderer; i.e. setting up shaders and fonts. Should be called within
	 * onSurfaceCreated(...)
	 */
	public static void setRenderer(OurGLRenderer renderer) {
		OurGLRenderer.ensureOpenGLThread();
		sRenderer = renderer;
		Context sContext = renderer.context();
		sVertexShader = GLShader.readVertexShader(sContext,
				R.raw.simple_vertex_shader);
		sFragmentShader = GLShader.readFragmentShader(sContext,
				R.raw.simple_fragment_shader);
		sProgram = GLProgram.build(sVertexShader, sFragmentShader);
		sFont = new Font(24);
		sLineWidth = 1.0f;
	}

	protected static void renderFrameTitle(String sFrameTitle) {
		sFont.render(sFrameTitle, new Point(10, 10 + sFont.lineHeight()));
	}

	public void setLineWidth0(float lineWidth) {
		mLineWidth = lineWidth;
	}

	public float lineWidth() {
		return mLineWidth;
	}

	private float mLineWidth;

	private static Font sFont;
	private static OurGLRenderer sRenderer;
	private static GLShader sVertexShader;
	private static GLShader sFragmentShader;
	private static GLProgram sProgram;
	private static Polyline sPolyline;
	private static float sLineWidth;
}
