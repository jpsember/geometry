package com.js.geometryapp;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class OurGLSurfaceView extends GLSurfaceView {

	public OurGLSurfaceView(Context context, GLSurfaceView.Renderer renderer) {
		super(context);
		setEGLContextClientVersion(2);
		setRenderer(renderer);
	}
}
