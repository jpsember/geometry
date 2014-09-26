package com.js.samplealgorithm;

import java.util.Random;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;

import com.js.geometry.*;
import com.js.geometryapp.AbstractWidget;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;

//import static com.js.geometry.MyMath.*;

import static com.js.basic.Tools.*;

public class DelaunayDriver implements AlgorithmStepper.Delegate {

	private static final String BGND_ELEMENT_MESH = "00";

	private static final int COLOR_LIGHTBLUE = Color.argb(80, 100, 100, 255);
	// private
	static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

	public DelaunayDriver(Context context) {
		doNothing();
		mStepper = AlgorithmStepper.sharedInstance();
	}

	public void setView(GLSurfaceView view) {
		mView = view;
	}

	private boolean update() {
		return mStepper.update();
	}

	private void show(Object message) {
		mStepper.show(message);
	}

	@Override
	public void runAlgorithm() {

		Rect r = new Rect(150, 150, 700, 700);
		try {
			mContext = GeometryContext.contextWithRandomSeed(sOptions
					.getIntValue("seed"));
			mRandom = mContext.random();
			if (mStepper.isActive()) {
				mStepper.plotToBackground(BGND_ELEMENT_MESH);
				mStepper.setLineWidth(1);
				mStepper.setColor(COLOR_LIGHTBLUE);
				mStepper.plot(mContext);
			}

			mDelaunay = new Delaunay(mContext);
			if (update())
				show("*Initial triangulation");

			int numPoints = sOptions.getIntValue("numpoints");

			for (int i = 0; i < numPoints; i++) {
				Point pt = new Point(r.x + mRandom.nextFloat() * r.width, r.y
						+ mRandom.nextFloat() * r.height);

				mDelaunay.add(pt);
			}
			if (update())
				show("*Inserted " + numPoints + " points");

		} catch (GeometryException e) {
			pr("\n\ncaught exception:\n" + e);
			mStepper.show("caught exception: " + e);
		}
	}

	@Override
	public void displayResults() {
		mView.requestRender();
	}

	@Override
	public void prepareOptions() {
		sOptions = AlgorithmOptions.sharedInstance();

		sOptions.addSlider("seed", 0, 300).addListener(
				AbstractWidget.LISTENER_UPDATE);
		sOptions.addSlider("numpoints", 1, 250).addListener(
				AbstractWidget.LISTENER_UPDATE);
		sOptions.addDetailBox(Delaunay.DETAIL_SWAPS);
		sOptions.addDetailBox(Delaunay.DETAIL_FIND_TRIANGLE);
	}

	private static AlgorithmOptions sOptions;
	private static AlgorithmStepper mStepper;

	private GeometryContext mContext;
	private Delaunay mDelaunay;
	private Random mRandom;
	private GLSurfaceView mView;
}
