package com.js.geometryapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import static com.js.basic.Tools.*;
import com.js.geometry.*;

public class OurGLSurfaceView extends GLSurfaceView {
	public OurGLSurfaceView(Context context) {
		super(context);

		mRenderer = new OurGLRenderer();
		setRenderer(mRenderer);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		// We must do this to suppress a warning
		if (mAlwaysFalseFlag)
			performClick();

		Point loc = new Point(event.getX(), event.getY());

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_UP:
			pr("up at " + loc);
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		}

		// if (false)
		// queueEvent(new Runnable() {
		// public void run() {
		// mRenderer.setColor(event.getX() / getWidth(),
		// event.getY() / getHeight(), 1.0f);
		// }
		// });
		return true;
	}

	@Override
	public boolean performClick() {
		return super.performClick();
	}

	private static boolean mAlwaysFalseFlag;
	private OurGLRenderer mRenderer;
}
