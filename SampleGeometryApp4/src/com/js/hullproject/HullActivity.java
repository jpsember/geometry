package com.js.hullproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.graphics.Color;

import com.js.geometry.Disc;
import com.js.geometry.GeometryException;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmDisplayElement;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GeometryStepperActivity;
import static com.js.basic.Tools.*;

public class HullActivity extends GeometryStepperActivity implements Algorithm {

	private static final String STEP_THROUGH_BITANGENTS = "Show Bitangent Calculations";
	private static final String ALTERNATE_BITANGENT_CALC = "Alternative Bitangent Calc";

	private static final String BGND_ELEMENT_BITANGENTS = "10";
	private static final String BGND_ELEMENT_CURRENTDISC = "20";
	private static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

	@Override
	public void addAlgorithms(AlgorithmStepper s) {
		s.addAlgorithm(this);
	}

	@Override
	public String getAlgorithmName() {
		return "Convex Hull of Discs";
	}

	@Override
	public void prepareOptions(AlgorithmOptions options) {
		options.addCheckBox(STEP_THROUGH_BITANGENTS);
		options.addCheckBox(ALTERNATE_BITANGENT_CALC);
		mOptions = options;
	}

	@Override
	public void prepareInput(AlgorithmInput input) {
		mDiscs.clear();
		for (Disc d : input.discs)
			mDiscs.add(d);
	}

	@Override
	public void run(AlgorithmStepper stepper) {
		this.s = stepper;

		if (mDiscs.size() < 2)
			s.show("Not enough discs");

		if (s.openLayer(BGND_ELEMENT_BITANGENTS)) {
			s.plot(new AlgorithmDisplayElement() {
				@Override
				public void render() {
					s.setColor(COLOR_DARKGREEN);
					Disc prevDisc = null;
					for (Disc d : mHullDiscs) {
						s.plot(d);
						if (prevDisc != null) {
							Bitangent b = calcBitangent2(prevDisc, d);
							if (b != null) {
								s.plotRay(b.maPoint, b.mbPoint);
							}
						}
						prevDisc = d;
					}
				}
			});
			s.closeLayer();
		}

		sortDiscsBySize();
		initializeBitangents();
		if (s.openLayer(BGND_ELEMENT_CURRENTDISC)) {
			if (mCurrentDisc != null) {
				s.setColor(COLOR_DARKGREEN);
				s.plot(mCurrentDisc);
			}
			s.closeLayer();
		}

		for (int i = mDiscs.size() - 1; i >= 0; i--) {
			Disc d = mDiscs.get(i);
			mCurrentDisc = d;
			processDisc(d);
			mCurrentDisc = null;
		}
		s.removeLayer(BGND_ELEMENT_CURRENTDISC);
	}

	private void sortDiscsBySize() {
		Collections.sort(mDiscs, new Comparator() {
			@Override
			public int compare(Object lhs, Object rhs) {
				return (int) Math.signum(((Disc) lhs).getRadius()
						- ((Disc) rhs).getRadius());
			}
		});

	}

	private void initializeBitangents() {
		mHullDiscs.clear();

		Disc leftmostDisc = mDiscs.get(0);
		Disc rightmostDisc = mDiscs.get(0);
		for (Disc d : mDiscs) {
			if (d.getOrigin().x - d.getRadius() < leftmostDisc.getOrigin().x
					- leftmostDisc.getRadius())
				leftmostDisc = d;
			if (d.getOrigin().x + d.getRadius() > rightmostDisc.getOrigin().x
					+ rightmostDisc.getRadius())
				rightmostDisc = d;
		}
		if (s.step())
			s.show("Leftmost and rightmost discs" + s.highlight(leftmostDisc)
					+ s.highlight(rightmostDisc));

		mHullDiscs.add(rightmostDisc);
		if (leftmostDisc != rightmostDisc)
			mHullDiscs.add(leftmostDisc);
	}

	/**
	 * Calculate bitangent, a directed ray tangent to discs da and db, with both
	 * discs lying to its left
	 * 
	 * Solves using analytical geometry
	 * 
	 * @return bitangent, two points on a directed line; or null if no bitangent
	 *         exists
	 */
	private Bitangent calcBitangent(Disc da, Disc db) {

		Bitangent bitangent = null;

		boolean exchangeDiscs = false;

		// If disc A is larger, we shrink both discs until B becomes a
		// point. Then we will calculate the bitangent of the shrunken A
		// with the origin of B.
		exchangeDiscs = (da.getRadius() < db.getRadius());
		// Let r be the radius of the shrunken A.
		float r = Math.abs(da.getRadius() - db.getRadius());

		if (exchangeDiscs) {
			Disc tmp = da;
			da = db;
			db = tmp;
		}

		Point aOrigin = da.getOrigin();
		Point bOrigin = db.getOrigin();

		// To simplify the calculations, translate both (shrunken) discs so
		// B is at the origin
		Point bTranslated = MyMath.subtract(bOrigin, aOrigin);

		// If disc a contains disc b, there is no bitangent.
		float bDistance = bTranslated.magnitude();
		if (bDistance + db.getRadius() <= da.getRadius())
			return null;

		/**
		 * <pre>
		 * 
		 * Let B' be the origin of the translated B.
		 * Let T be the point of tangency of the bisector with (shrunken) A.
		 * 
		 * We have:
		 * 
		 * Tx^2 + Ty^2 = r^2
		 * 
		 * T . (B' - C) = 0
		 * 
		 * Algebra yields:
		 * 
		 * Tx^2(B'y^2 + B'x^2) + Tx(-2*r^2*B'x) + (r^4 - r^2 * B'y^2) = 0
		 * 
		 * which is a quadratic equation in one variable, Tx.  We solve for that,
		 * then solve for Ty as
		 * 
		 * Ty = (r^2 - B'x*Tx) / B'y
		 * 
		 * </pre>
		 */

		float rSquared = r * r;
		float qa = (bTranslated.y * bTranslated.y + bTranslated.x
				* bTranslated.x);
		float qb = -2 * bTranslated.x * rSquared;
		float qc = rSquared * (rSquared - bTranslated.y * bTranslated.y);

		float[] roots = MyMath.solveQuadratic(qa, qb, qc, null);

		Point tangent = new Point(roots[0],
				(rSquared - (bTranslated.x * roots[0])) / bTranslated.y);

		// Determine if this is the correct root by seeing which side of the
		// radial (0-->T) A' lies upon.
		// If we exchanged discs, invert this test.
		boolean switchRoots = (MyMath.sideOfLine(Point.ZERO, tangent,
				bTranslated) < 0);
		switchRoots ^= exchangeDiscs;

		if (switchRoots)
			tangent.setTo(roots[1], (rSquared - (bTranslated.x * roots[1]))
					/ bTranslated.y);

		// Figure out the translation to apply to the bitangent to undo
		// the effect of shrinking the discs initially
		Point radialAdjust;
		if (r == 0) {
			// Both discs were the same size, and shrunk to points;
			// rotate the B vector 90 degrees CCW and scale appropriately to
			// find tangent point
			float rScale = da.getRadius() / bDistance;
			radialAdjust = new Point(bTranslated.y * rScale, -bTranslated.x
					* rScale);
		} else {
			float rScale = db.getRadius() / r;
			radialAdjust = new Point(rScale * tangent.x, rScale * tangent.y);
		}

		Point aTangentPoint = new Point(aOrigin);
		aTangentPoint.add(tangent);
		aTangentPoint.add(radialAdjust);
		Point bTangentPoint = new Point(bOrigin);
		bTangentPoint.add(radialAdjust);

		if (!exchangeDiscs) {
			bitangent = new Bitangent(aTangentPoint, bTangentPoint);
		} else {
			bitangent = new Bitangent(bTangentPoint, aTangentPoint);
		}

		return bitangent;
	}

	/**
	 * Calculate bitangent, a directed ray tangent to discs da and db, with both
	 * discs lying to its left
	 * 
	 * Solves using trigonometry (simpler and probably more robust)
	 * 
	 * @return bitangent, two points on a directed line; or null if no bitangent
	 *         exists
	 */
	private Bitangent calcBitangent2(Disc da, Disc db) {
		Bitangent bitangent = null;
		s.pushActive(mOptions.getBooleanValue(STEP_THROUGH_BITANGENTS));
		do {
			if (mOptions.getBooleanValue(ALTERNATE_BITANGENT_CALC)) {
				bitangent = calcBitangent(da, db);
				break;
			}
			boolean exchangeDiscs = false;

			if (s.step())
				s.show("Calc bitangent from" + s.highlight(da));
			if (s.step())
				s.show("Calc bitangent to" + s.highlight(db));

			// Proceed assuming da is the larger of the two
			exchangeDiscs = (da.getRadius() < db.getRadius());
			if (s.step())
				s.show(" exchangeDiscs " + d(exchangeDiscs));

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
				break;

			float theta = MyMath.polarAngle(bOrigin.x - aOrigin.x, bOrigin.y
					- aOrigin.y);
			float phi = MyMath.M_DEG
					* 90
					- (float) Math.asin((da.getRadius() - db.getRadius())
							/ abDist);
			float alpha;
			if (!exchangeDiscs) {
				alpha = theta - phi;
			} else {
				alpha = theta + phi;
			}
			if (s.step())
				s.show(" theta=" + da(theta) + " phi=" + da(phi) + " alpha="
						+ da(alpha));

			Point aTangentPoint, bTangentPoint;
			aTangentPoint = MyMath
					.pointOnCircle(aOrigin, alpha, da.getRadius());
			bTangentPoint = MyMath
					.pointOnCircle(bOrigin, alpha, db.getRadius());

			if (!exchangeDiscs) {
				bitangent = new Bitangent(aTangentPoint, bTangentPoint);
			} else {
				bitangent = new Bitangent(bTangentPoint, aTangentPoint);
			}
			if (s.step())
				s.show("Bitangent" + highlight(bitangent));
		} while (false);
		s.popActive();
		return bitangent;
	}

	private void processDisc(Disc xDisc) {
		if (s.step())
			s.show("Processing next remaining largest disc"
					+ s.highlight(xDisc));

		// Find new hull bitangent XU that this disc supports, if possible.

		// Find first adjacent pair of discs UV such that bitangent UX exists,
		// is an upper hull candidate, and is cw of UV's bitangent.
		// If U is the last hull disc, treat as if it supports a bitangent
		// pointing downward

		Bitangent ux = null;
		Disc uDisc = null;
		int i, j;
		for (i = 0; i < mHullDiscs.size(); i++) {
			uDisc = mHullDiscs.get(i);
			ux = calcBitangent2(uDisc, xDisc);
			if (s.step())
				s.show("Looking for hull bitangent UX" + s.highlight(uDisc));
			if (ux == null) {
				if (s.step())
					s.show("no bitangent exists with " + s.highlight(uDisc));
				return;
			}
			if (!hullAngle(ux.pseudoAngle())) {
				if (s.step())
					s.show("ux, not a hull disc, dominated "
							+ da(ux.pseudoAngle()) + highlight(ux)
							+ s.highlight(uDisc));
				return;
			}

			float angle;
			if (i + 1 < mHullDiscs.size()) {
				Disc vDisc = mHullDiscs.get(i + 1);
				Bitangent uv = calcBitangent2(uDisc, vDisc);
				ASSERT(uv != null);
				angle = uv.pseudoAngle();
			} else {
				angle = -MyMath.PSEUDO_ANGLE_RANGE_14;
			}
			if (MyMath.pseudoAngleIsConvex(ux.pseudoAngle(), angle)) {
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

			xv = calcBitangent2(xDisc, vDisc);
			if (xv == null) {
				if (s.step())
					s.show("no bitangent exists with " + s.highlight(vDisc));
				return;
			}
			if (!hullAngle(xv.pseudoAngle())) {
				if (s.step())
					s.show("not a hull disc, dominated" + da(xv.pseudoAngle())
							+ highlight(xv) + s.highlight(vDisc));
				return;
			}

			float angle;
			if (j - 1 >= 0) {
				Disc uDisc2 = mHullDiscs.get(j - 1);
				Bitangent uv = calcBitangent2(uDisc2, vDisc);
				ASSERT(uv != null);
				angle = uv.pseudoAngle();
			} else {
				angle = MyMath.PSEUDO_ANGLE_RANGE_14;
			}
			if (MyMath.pseudoAngleIsConvex(angle, xv.pseudoAngle()))
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

	private String highlight(Bitangent bitangent) {
		s.highlightRay(bitangent.maPoint, bitangent.mbPoint);
		return "";
	}

	/**
	 * Determine if a (normalized) bitangent pseudoangle is an upper hull
	 * candidate
	 */
	private boolean hullAngle(float pseudoAngle) {
		return pseudoAngle >= MyMath.PSEUDO_ANGLE_RANGE_14
				|| pseudoAngle <= -MyMath.PSEUDO_ANGLE_RANGE_14;
	}

	private static class Bitangent {
		public Bitangent(Point aTangent, Point bTangent) {
			maPoint = aTangent;
			mbPoint = bTangent;
			mAngle = MyMath.pseudoPolarAngle(MyMath.subtract(mbPoint, maPoint));
		}

		public float pseudoAngle() {
			return mAngle;
		}

		public float mAngle;
		public Point maPoint, mbPoint;
	}

	private List<Disc> mDiscs = new ArrayList();
	private ArrayList<Disc> mHullDiscs = new ArrayList();

	private AlgorithmOptions mOptions;
	private AlgorithmStepper s;
	private Disc mCurrentDisc;
}
