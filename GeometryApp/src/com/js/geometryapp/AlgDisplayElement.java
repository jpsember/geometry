package com.js.geometryapp;

import android.content.Context;

import com.js.geometry.Point;
import com.js.geometry.R;

public abstract class AlgDisplayElement {

	public abstract void render();

	public static void startPolyline(Point point) {
		sPolyline = new Polyline();
		sPolyline.setColor(1.0f, 0.3f, 0.3f);
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
	}

	protected static void renderFrameTitle(String sFrameTitle) {
		sFont.render(sFrameTitle, new Point(10, 10 + sFont.lineHeight()));
	}

	private static Font sFont;

	private static OurGLRenderer sRenderer;
	private static GLShader sVertexShader;
	private static GLShader sFragmentShader;
	private static GLProgram sProgram;
	private static Polyline sPolyline;
}
