package com.js.geometryapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
//import android.view.MotionEvent;
//import static com.js.basic.Tools.*;
//
//import com.js.geometry.*;

public class OurGLSurfaceView extends GLSurfaceView {

	public OurGLSurfaceView(Context context, GLSurfaceView.Renderer renderer) {
		super(context);
		setEGLContextClientVersion(2);
			setRenderer(renderer);
		// setRenderer(new OurGLRenderer(this.getContext()));
	}

	// public void setSampleContext(GeometryContext c) {
	// ASSERT(mRenderer != null);
	// mRenderer.setSampleContext(c);
	// }


	// @Override
	// public boolean onTouchEvent(final MotionEvent event) {
	//
	// // We must do this to suppress a warning
	// if (mAlwaysFalseFlag)
	// performClick();
	//
	// Point loc = new Point(event.getX(), event.getY());
	//
	// switch (event.getAction()) {
	// case MotionEvent.ACTION_DOWN:
	// break;
	// case MotionEvent.ACTION_UP:
	// pr("up at " + loc);
	// queueEvent(new Runnable() {
	// public void run() {
	// mRenderer.bumpScale();
	// }
	// });
	// break;
	//
	// case MotionEvent.ACTION_MOVE:
	// break;
	// }
	//
	// return true;
	// }
	//
	// @Override
	// public boolean performClick() {
	// return super.performClick();
	// }

	// private OurGLRenderer mRenderer;
	static boolean mAlwaysFalseFlag;
}
