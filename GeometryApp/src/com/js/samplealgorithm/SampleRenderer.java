package com.js.samplealgorithm;

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
	private static final boolean ADD_TEST_POINTS = false;

	public SampleRenderer(Context context, SampleAlgorithm algorithm) {
		super(context);
		mStepper = AlgorithmStepper.sharedInstance();
		mAlgorithm = algorithm;
		doNothing();
	}

	@Override
	public void onSurfaceChanged() {

		// Only generate test polygons if we're in landscape mode
		if (ADD_TEST_POLYGON && deviceSize().x > deviceSize().y) {
			mConvexPolygon = Polygon.discPolygon(Point.ZERO, 100, 23);
			mConvexPolygon.transformToFitRect(new Rect(200, 200, 300, 120),
					false);
			mConvexPolygonMesh = PolygonMesh
					.meshForConvexPolygon(mConvexPolygon);

			GeometryContext c = new GeometryContext(11);
			mNonConvexPolygon = Polygon.testPolygon(c,
					false ? Polygon.TESTPOLY_DRAGON_X + 5
							: Polygon.TESTPOLY_CONCAVE_BLOB);
			mNonConvexPolygon
					.transformToFitRect(new Rect(0, 300, algorithmRect().width,
							algorithmRect().height - 300), false);
			mNonConvexPolygonMesh = PolygonMesh
					.meshForSimplePolygon(mNonConvexPolygon);

			mPolygonRenderer = new PolygonProgram(this,
					TRANSFORM_NAME_ALGORITHM_TO_NDC);

		}

		if (ADD_TEST_SPRITE) {
			GLTexture t = new GLTexture(context(), R.raw.texture);
			Rect window = new Rect(0, 0, t.width(), t.height());

			mSprite = new GLSpriteProgram(SpriteContext.normalContext(), t,
					window);
		}
	}

	@Override
	public void onDrawFrame() {

		mStepper.render();

		if (mSprite != null) {
			int frame = mAlgorithm.getFrameNumber();
			Point pt = MyMath.pointOnCircle(new Point(250, 100), frame * 7.0f
					* MyMath.M_DEG, 100);
			mSprite.setPosition(pt.x, pt.y);
			mSprite.render();
		}

		if (mConvexPolygon != null) {
			{
				int duration = 8;
				int step = mFrame % (duration * 2);
				if (step % duration == 0) {
					mPolygonRenderer.setColor(step > 0 ? Color.argb(120, 120,
							120, 250) : Color.argb(120, 200, 200, 200));
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

		if (ADD_TEST_POINTS) {
			AlgDisplayElement.setColorState(Color.BLUE);

			Point origin = new Point(500, 500);
			AlgDisplayElement.renderPoint(origin);
			AlgDisplayElement.renderPoint(MyMath.pointOnCircle(origin,
					(mFrame / 60.0f) * MyMath.PI * 2, 100));

			// Do same thing, with different origin and radius
			origin = new Point(500, 800);
			AlgDisplayElement.renderPoint(origin, 2.5f);
			AlgDisplayElement.renderPoint(
					MyMath.pointOnCircle(origin, (mFrame / 60.0f) * MyMath.PI
							* 2, 100), 2.5f);
		}

		mFrame++;
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
