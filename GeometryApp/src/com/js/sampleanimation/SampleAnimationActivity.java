package com.js.sampleanimation;

import static com.js.basic.Tools.*;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.geometry.Rect;
import com.js.geometryapp.GeometryActivity;
import com.js.opengl.GLSpriteProgram;
import com.js.opengl.GLTexture;
import com.js.opengl.OurGLRenderer;
import com.js.opengl.OurGLSurfaceView;
import com.js.opengl.SpriteContext;

public class SampleAnimationActivity extends GeometryActivity {

	protected GLSurfaceView buildOpenGLView() {
		GLSurfaceView v = new OurGLSurfaceView(this, new SampleRenderer(this));
		return v;
	}

	/**
	 * Define a custom renderer
	 */
	private static class SampleRenderer extends OurGLRenderer {

		public SampleRenderer(Context context) {
			super(context);
			doNothing();
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int w, int h) {
			super.onSurfaceChanged(gl, w, h);
			mSpriteContext = new SpriteContext(TRANSFORM_NAME_DEVICE_TO_NDC,
					false);
			GLTexture t = new GLTexture(context(), R.raw.texture, true);
			mSpriteProgram = new GLSpriteProgram(mSpriteContext, t, new Rect(0,
					0, t.width(), t.height()));

			mSpriteContext2 = new SpriteContext(TRANSFORM_NAME_DEVICE_TO_NDC,
					true);
			GLTexture t2 = new GLTexture(context(), R.raw.squareicon, true);
			mSpriteProgram2 = new GLSpriteProgram(mSpriteContext2, t2,
					new Rect(0, 0, t2.width(), t2.height()));
		}

		@Override
		public void onDrawFrame(GL10 gl) {
			super.onDrawFrame(gl);
			mFrame++;
			mSpriteProgram.setPosition(MyMath.pointOnCircle(
					new Point(250, 500), mFrame * 5 * MyMath.M_DEG, 200));
			mSpriteProgram.render();

			mSpriteContext2.setTintColor(Color.RED);
			mSpriteProgram2.setPosition(new Point(200, 200));
			mSpriteProgram2.render();

		}

		private int mFrame;
		private SpriteContext mSpriteContext;
		private SpriteContext mSpriteContext2;
		private GLSpriteProgram mSpriteProgram;
		private GLSpriteProgram mSpriteProgram2;

	}
}
