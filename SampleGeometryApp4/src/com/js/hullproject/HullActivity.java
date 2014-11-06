package com.js.hullproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.graphics.Color;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Disc;
import com.js.geometry.GeometryException;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.geometry.Renderable;
import com.js.geometry.Segment;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.RenderTools;

public class HullActivity extends GeometryStepperActivity implements Algorithm {

	private static final String INCLUDE_LOWER_HULL = "Include lower hull";
	private static final String USE_EDITOR_DISCS = "Use editor discs";
	private static final String NUMBER_OF_DISCS = "# discs";
	private static final String DISC_SIZE = "Radius";
	private static final String RANDOM_SEED = "Seed";

	private static final String BGND_ELEMENT_BITANGENTS = "10";
	private static final String BGND_ELEMENT_CURRENTDISC = "20";
	private static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

	@Override
	public void addAlgorithms(AlgorithmStepper stepper) {
		stepper.addAlgorithm(this);
	}

	@Override
	public String getAlgorithmName() {
		return "Convex Hull of Discs";
	}

	@Override
	public void prepareOptions(AlgorithmOptions options) {
		options.addCheckBox(USE_EDITOR_DISCS);
		options.addCheckBox(INCLUDE_LOWER_HULL);
		options.addSlider(NUMBER_OF_DISCS, "min", 2, "max", 150, "value", 25);
		options.addSlider(RANDOM_SEED, "min", 1, "max", 100);
		options.addSlider(DISC_SIZE, "min", 0, "max", 100, "value", 20);

		mOptions = options;
	}

	private void prepareInput(AlgorithmInput input) {
		mAlgBounds = input.algorithmRect;
		mDiscs.clear();
		if (mOptions.getBooleanValue(USE_EDITOR_DISCS)) {
			for (Disc d : input.discs)
				mDiscs.add(d);
		} else {
			// Generate some random discs
			Random r = new Random(mOptions.getIntValue(RANDOM_SEED));
			float pow = (100 - (float) mOptions.getIntValue(DISC_SIZE)) / 8;
			for (int i = mOptions.getIntValue(NUMBER_OF_DISCS); i >= 0; i--) {
				Point origin = MyMath.randomPointInDisc(r,
						mAlgBounds.midPoint(), mAlgBounds.maxDim() * .4f);
				mDiscs.add(new Disc(origin,
						(float) Math.pow(r.nextFloat(), pow)
								* mAlgBounds.minDim() * .4f
								+ mAlgBounds.minDim() * .01f));
			}
		}
	}

	@Override
	public void run(AlgorithmStepper stepper, AlgorithmInput input) {
		prepareInput(input);
		s = stepper;
		if (mDiscs.size() < 2) {
			s.show("Not enough discs");
		}
		mHullDiscLists = new List[2];
		mHullDiscLists[0] = new ArrayList();
		mHullDiscLists[1] = new ArrayList();
		mRendering_DiscsExamined = new HashSet();
		mRendering_CurrentDisc = null;

		s.addLayer(BGND_ELEMENT_BITANGENTS, new Renderable() {
			@Override
			public void render(AlgorithmStepper s) {
				// Keep track of which discs appeared as hull discs
				Set<Disc> hullDiscsFound = new HashSet();
				List<Bitangent> bitangentsFound = new ArrayList();
				s.setColor(COLOR_DARKGREEN);
				for (int pass = 0; pass < 2; pass++) {
					List<Disc> hullDiscList = mHullDiscLists[pass];
					Disc prevDisc = null;
					for (Disc d : hullDiscList) {
						hullDiscsFound.add(d);
						s.plot(d);
						if (prevDisc != null) {
							Bitangent b = constructBitangent(prevDisc, d);
							if (b != null)
								bitangentsFound.add(b);
						}
						prevDisc = d;
					}
				}
				// Plot discs: gray if unexamined, blue if examined and not
				// on hull, green if examined and on hull
				for (Disc d : mDiscs) {
					// If current disc, it's already been rendered
					if (d == mRendering_CurrentDisc)
						continue;
					if (hullDiscsFound.contains(d))
						s.setColor(COLOR_DARKGREEN);
					else if (!mRendering_DiscsExamined.contains(d))
						s.setColor(Color.LTGRAY);
					else
						s.setColor(Color.BLUE);
					s.plot(d);
				}
				// Plot hull bitangents
				s.setColor(COLOR_DARKGREEN);
				for (Bitangent b : bitangentsFound)
					s.plot(b);
			}
		});

		sortDiscsBySize();
		for (int pass = 0; pass < 2; pass++) {
			mLowerHullFlag = (pass == 1);
			if (mLowerHullFlag && !mOptions.getBooleanValue(INCLUDE_LOWER_HULL))
				continue;
			if (s.bigStep())
				s.show("Constructing " + (mLowerHullFlag ? "lower" : "upper")
						+ " hull");
			mHullDiscs = mHullDiscLists[pass];
			initializeHullDiscList();

			for (int i = mDiscs.size() - 1; i >= 0; i--) {
				Disc d = mDiscs.get(i);
				mRendering_CurrentDisc = d;
				s.addLayer(BGND_ELEMENT_CURRENTDISC, RenderTools
						.buildHighlightingRenderable(mRendering_CurrentDisc));
				processDisc(d);
				mRendering_DiscsExamined.add(d);
				mRendering_CurrentDisc = null;
				s.removeLayer(BGND_ELEMENT_CURRENTDISC);
			}
		}
	}

	private void sortDiscsBySize() {
		Collections.sort(mDiscs, new Comparator() {
			public int compare(Object lhs, Object rhs) {
				return (int) Math.signum(((Disc) lhs).getRadius()
						- ((Disc) rhs).getRadius());
			}
		});
	}

	/**
	 * Calculate leftmost point on disc
	 */
	private float leftmostPoint(Disc d) {
		return d.getOrigin().x - d.getRadius();
	}

	/**
	 * Calculate rightmost point on disc
	 */
	private float rightmostPoint(Disc d) {
		return d.getOrigin().x + d.getRadius();
	}

	private void initializeHullDiscList() {
		mHullDiscs.clear();

		Disc discLeft = mDiscs.get(0);
		Disc discRight = discLeft;
		for (Disc disc : mDiscs) {
			if (leftmostPoint(disc) < leftmostPoint(discLeft))
				discLeft = disc;
			if (rightmostPoint(disc) > rightmostPoint(discRight))
				discRight = disc;
		}
		if (mLowerHullFlag) {
			Disc temp = discLeft;
			discLeft = discRight;
			discRight = temp;
		}
		mHullDiscs.add(discRight);
		if (discLeft != discRight)
			mHullDiscs.add(discLeft);
		if (s.step())
			s.show("Extremal discs" + s.highlight(discLeft)
					+ s.highlight(discRight));
	}

	/**
	 * Calculate bitangent, a directed ray tangent to discs da and db, with both
	 * discs lying to its left
	 * 
	 * @return bitangent, two points on a directed line; or null if no bitangent
	 *         exists
	 */
	private Bitangent constructBitangent(Disc da, Disc db) {
		boolean exchangeDiscs = false;

		// Proceed assuming da is the larger of the two
		exchangeDiscs = (da.getRadius() < db.getRadius());
		if (exchangeDiscs) {
			Disc tmp = da;
			da = db;
			db = tmp;
		}

		Point aOrigin = da.getOrigin();
		Point bOrigin = db.getOrigin();

		float abDist = MyMath.distanceBetween(aOrigin, bOrigin);
		// If disc a contains disc b, there is no bitangent.
		if (abDist + db.getRadius() <= da.getRadius())
			return null;

		float theta = MyMath.polarAngle(bOrigin.x - aOrigin.x, bOrigin.y
				- aOrigin.y);
		float phi = MyMath.M_DEG * 90
				- (float) Math.asin((da.getRadius() - db.getRadius()) / abDist);
		float alpha;
		if (!exchangeDiscs) {
			alpha = theta - phi;
		} else {
			alpha = theta + phi;
		}

		Point aTangentPoint, bTangentPoint;
		aTangentPoint = MyMath.pointOnCircle(aOrigin, alpha, da.getRadius());
		bTangentPoint = MyMath.pointOnCircle(bOrigin, alpha, db.getRadius());

		Bitangent bitangent = null;
		if (!exchangeDiscs) {
			bitangent = new Bitangent(aTangentPoint, bTangentPoint);
		} else {
			bitangent = new Bitangent(bTangentPoint, aTangentPoint);
		}
		return bitangent;
	}

	private void processDisc(Disc xDisc) {
		if (s.step())
			s.show("Processing next remaining largest disc");

		// Find new hull bitangent XU that this disc supports, if possible.

		// Find first adjacent pair of discs UV such that bitangent UX exists,
		// is an upper hull candidate, and is cw of UV's bitangent.

		Bitangent ux = null;
		Disc uDisc = null;
		int i, j;
		for (i = 0; i < mHullDiscs.size(); i++) {
			uDisc = mHullDiscs.get(i);
			ux = constructBitangent(uDisc, xDisc);
			if (s.step())
				s.show("Looking for hull bitangent UX" + s.highlight(uDisc));
			if (ux == null) {
				if (s.step())
					s.show("no bitangent exists" + s.highlight(uDisc));
				return;
			}
			if (!isHullAngle(ux.angle())) {
				if (s.step())
					s.show("not a hull bitangent candidate"
							+ s.highlight(uDisc) + highlight(ux));
				return;
			}

			float angle;
			if (i + 1 < mHullDiscs.size()) {
				Disc vDisc = mHullDiscs.get(i + 1);
				Bitangent uv = constructBitangent(uDisc, vDisc);
				angle = uv.angle();
			} else {
				// U is the last hull disc, so treat as if it supports a
				// bitangent with the maximum hull angle
				angle = maxHullAngle();
			}
			if (MyMath.angleIsConvex(ux.angle(), angle)) {
				break;
			}
		}
		if (i == mHullDiscs.size())
			GeometryException.raise("unexpected");

		// Find hull bitangent XV this disc supports.
		Bitangent xv = null;
		Disc vDisc = null;
		for (j = mHullDiscs.size() - 1; j >= 0; j--) {
			vDisc = mHullDiscs.get(j);
			if (s.step())
				s.show("Looking for hull bitangent XV" + s.highlight(vDisc));

			xv = constructBitangent(xDisc, vDisc);
			if (xv == null) {
				if (s.step())
					s.show("no bitangent exists with " + s.highlight(vDisc));
				return;
			}
			if (!isHullAngle(xv.angle())) {
				if (s.step())
					s.show("not a hull bitangent candidate"
							+ s.highlight(vDisc) + highlight(xv));
				return;
			}

			float angle;
			if (j - 1 >= 0) {
				Disc uDisc2 = mHullDiscs.get(j - 1);
				Bitangent uv = constructBitangent(uDisc2, vDisc);
				angle = uv.angle();
			} else {
				// V is the first hull disc, so treat as if it supports a
				// bitangent with the minimum hull angle
				angle = minHullAngle();
			}
			if (MyMath.angleIsConvex(angle, xv.angle()))
				break;
		}
		if (j < 0)
			GeometryException.raise("unexpected");

		int remCount = j - i - 1;
		for (int k = 0; k < remCount; k++) {
			if (s.step())
				s.show("Removing hull disc "
						+ s.highlight(mHullDiscs.get(i + 1)));
			mHullDiscs.remove(i + 1);
		}
		if (s.step())
			s.show("Inserting hull disc" + s.highlight(xDisc));
		mHullDiscs.add(i + 1, xDisc);
	}

	/**
	 * Utility method to highlight a bitangent as a ray
	 */
	private String highlight(Bitangent bitangent) {
		s.highlight(Segment.directed(bitangent.maPoint, bitangent.mbPoint));
		return "";
	}

	/**
	 * Get minimum (normalized) angle of an upper hull bitangent
	 */
	private float minHullAngle() {
		return mLowerHullFlag ? MyMath.M_DEG * -90 : MyMath.M_DEG * 90;
	}

	/**
	 * Get minimum (normalized) angle of an upper hull bitangent
	 */
	private float maxHullAngle() {
		return mLowerHullFlag ? MyMath.M_DEG * 90 : MyMath.M_DEG * -90;
	}

	/**
	 * Determine if a (normalized) bitangent angle is an upper hull candidate
	 */
	private boolean isHullAngle(float angle) {
		return MyMath.angleIsConvex(minHullAngle(), angle);
	}

	private static class Bitangent implements Renderable {
		public Bitangent(Point aTangent, Point bTangent) {
			maPoint = aTangent;
			mbPoint = bTangent;
			mAngle = MyMath.polarAngle(MyMath.subtract(mbPoint, maPoint));
		}

		public float angle() {
			return mAngle;
		}

		@Override
		public void render(AlgorithmStepper stepper) {
			Segment.directed(maPoint, mbPoint).render(stepper);
		}

		public float mAngle;
		public Point maPoint, mbPoint;
	}

	private AlgorithmOptions mOptions;
	private Rect mAlgBounds;
	private AlgorithmStepper s;
	private List<Disc> mDiscs = new ArrayList();
	// Two lists, one for upper, one for lower hull
	private List<Disc> mHullDiscLists[];
	private boolean mLowerHullFlag;
	// Active list (i.e. either upper or lower hull)
	private List<Disc> mHullDiscs;
	// For rendering purposes only, set of discs examined (for either hull)
	private Set<Disc> mRendering_DiscsExamined;
	private Disc mRendering_CurrentDisc;
}
