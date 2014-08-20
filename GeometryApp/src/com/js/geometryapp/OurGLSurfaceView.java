package com.js.geometryapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class OurGLSurfaceView extends GLSurfaceView {
	public OurGLSurfaceView(Context context) {
		super(context);

		mRenderer = new OurGLRenderer();
		mRenderer.setColor(.2f, .6f, .2f);
		setRenderer(mRenderer);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		// This is to get rid of compile warnings:
		if (event == null)
			this.performClick();
		// ---------------------------------------

		queueEvent(new Runnable() {
			public void run() {
				mRenderer.setColor(event.getX() / getWidth(), event.getY()
						/ getHeight(), 1.0f);
			}
		});
		return true;
	}

	// Override this as well to get rid of compile warning
	@Override
	public boolean performClick() {
		return super.performClick();
	}

	private OurGLRenderer mRenderer;
}
