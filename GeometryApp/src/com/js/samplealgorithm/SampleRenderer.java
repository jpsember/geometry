package com.js.samplealgorithm;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.R;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgDisplayElement;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GLSpriteProgram;
import com.js.geometryapp.GLTexture;
import com.js.geometryapp.PolygonMesh;
import com.js.geometryapp.PolygonProgram;
import com.js.geometryapp.SpriteContext;

import android.content.Context;
import android.graphics.Color;

public class SampleRenderer extends AlgorithmRenderer {

	private static final boolean ADD_TEST_SPRITE = false;
	private static final boolean ADD_TEST_POLYGON = true;

	public SampleRenderer(Context context, SampleAlgorithm algorithm) {
		super(context);
		mStepper = AlgorithmStepper.sharedInstance();
		mAlgorithm = algorithm;
		doNothing();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
		// Let the algorithm stepper elements prepare using this renderer
		AlgDisplayElement.setRenderer(this);

		// Synchronize with stepper, so no race conditions with UI thread.
		synchronized (mStepper) {

			if (ADD_TEST_POLYGON) {
				warning("adding polygon for test purposes");
				mConvexPolygon = Polygon.discPolygon(Point.ZERO, 100, 13);
				mConvexPolygon.transformToFitRect(new Rect(200, 200, 300, 120),
						false);
				mConvexPolygonMesh = PolygonMesh
						.meshForConvexPolygon(mConvexPolygon);

				GeometryContext c = new GeometryContext(11);
				mNonConvexPolygon = Polygon.testPolygon(c,
						Polygon.TESTPOLY_CONCAVE_BLOB);
				mNonConvexPolygon.transformToFitRect(new Rect(0, 300,
						algorithmRect().width, algorithmRect().height - 300),
						false);
				mNonConvexPolygonMesh = PolygonMesh
						.meshForSimplePolygon(mNonConvexPolygon);

				mPolygonRenderer = new PolygonProgram(this,
						TRANSFORM_NAME_ALGORITHM_TO_NDC);

			}

			if (ADD_TEST_SPRITE) {
				warning("adding sprite for test purposes");
				GLTexture t = new GLTexture(context(), R.raw.texture);
				Rect window = new Rect(0, 0, t.width(), t.height());

				mSprite = new GLSpriteProgram(SpriteContext.normalContext(), t,
						window);
			}
		}
	}

	public void onDrawFrame(GL10 gl) {

		// Synchronize with stepper, so no race conditions with UI thread.
		synchronized (mStepper) {
			super.onDrawFrame(gl);

			mStepper.render();

			if (mSprite != null) {
				int frame = mAlgorithm.getFrameNumber();
				Point pt = MyMath.pointOnCircle(new Point(250, 100), frame
						* 7.0f * MyMath.M_DEG, 100);
				mSprite.setPosition(pt.x, pt.y);
				mSprite.render();
			}

			if (mConvexPolygon != null) {
				{
					int duration = 8;
					int step = mFrame % (duration * 2);
					if (step % duration == 0) {
						mPolygonRenderer
								.setColor(step > 0 ? Color.argb(120, 120, 120,
										250) : Color.argb(120, 200, 200, 200));
					}
				}

				Point offset = null;
				{
					int duration = 13;
					int step = mFrame % (duration * 2);
					if (step >= duration) {
						float t = ((step - duration) / (float) duration)
								* algorithmRect().width;
						offset = new Point(t, t * .3f);
					}
				}

				mPolygonRenderer.render(mConvexPolygonMesh, offset);
				mPolygonRenderer.render(mNonConvexPolygonMesh);
			}

			mFrame++;
		}
	}

	private int mFrame;
	private PolygonProgram mPolygonRenderer;
	private GLSpriteProgram mSprite;
	private Polygon mConvexPolygon;
	private Polygon mNonConvexPolygon;
	private PolygonMesh mConvexPolygonMesh;
	private PolygonMesh mNonConvexPolygonMesh;
	private SampleAlgorithm mAlgorithm;
	private AlgorithmStepper mStepper;
}
