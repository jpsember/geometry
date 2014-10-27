package com.js.hullproject;

import com.js.geometry.Disc;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GeometryStepperActivity;
import static com.js.basic.Tools.*;

public class HullActivity extends GeometryStepperActivity implements Algorithm {

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
		mOptions = options;
	}

	@Override
	public void prepareInput(AlgorithmInput input) {
		mDiscs = input.discs;
	}

	@Override
	public void run(AlgorithmStepper s) {
		mStepper = s;

		if (mDiscs.length != 2)
			s.show("Not exactly two discs");

		Disc da = mDiscs[0];
		Disc db = mDiscs[1];
		if (s.step())
			s.show("Calc bitangent for " + s.highlight(da) + s.highlight(db));

		Point[] b;
		if (false)
			b = calcBitangent(da, db);
		else
			b = calcBitangent2(da, db);
		if (b == null) {
			if (s.step())
				s.show("No bitangent found");
		} else {
			if (s.step())
				s.show("Bitangent" + s.highlightLine(b[0], b[1]));
		}
		if (DEBUG_ONLY_FEATURES) {
		}
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
	private Point[] calcBitangent(Disc da, Disc db) {

		AlgorithmStepper s = mStepper;

		Point[] bitangent = null;

		boolean exchangeDiscs = false;

		if (s.step())
			s.show("Calc bitangents");

		// If disc B is larger, we shrink both discs until A becomes a
		// point. Then we will calculate the bitangent of the shrunken B
		// with the origin of A.
		exchangeDiscs = (db.getRadius() < da.getRadius());
		// Let r be the radius of the shrunken B.
		float r = Math.abs(db.getRadius() - da.getRadius());

		if (exchangeDiscs) {
			Disc tmp = da;
			da = db;
			db = tmp;
		}

		Point aOrigin = da.getOrigin();
		Point bOrigin = db.getOrigin();

		// To simplify the calculations, translate both (shrunken) discs so
		// B is at the origin
		Point aTranslated = MyMath.subtract(aOrigin, bOrigin);

		// If disc b contains disc a, there is no bitangent.
		float aDistance = aTranslated.magnitude();
		if (aDistance + da.getRadius() <= db.getRadius())
			return null;

		/**
		 * <pre>
		 * 
		 * Let A' be the origin of the translated A.
		 * Let T be the point of tangency of the bisector with (shrunken) B.
		 * 
		 * We have:
		 * 
		 * Tx^2 + Ty^2 = r^2
		 * 
		 * T . (A' - C) = 0
		 * 
		 * Algebra yields:
		 * 
		 * Tx^2(A'y^2 + A'x^2) + Tx(-2*r^2*A'x) + (r^4 - r^2 * A'y^2) = 0
		 * 
		 * which is a quadratic equation in one variable, Tx.  We solve for that,
		 * then solve for Ty as
		 * 
		 * Ty = (r^2 - A'x*Tx) / A'y
		 * 
		 * </pre>
		 */

		float rSquared = r * r;
		float qa = (aTranslated.y * aTranslated.y + aTranslated.x
				* aTranslated.x);
		float qb = -2 * aTranslated.x * rSquared;
		float qc = rSquared * (rSquared - aTranslated.y * aTranslated.y);

		float[] roots = MyMath.solveQuadratic(qa, qb, qc, null);

		Point tangent = new Point(roots[0],
				(rSquared - (aTranslated.x * roots[0])) / aTranslated.y);

		// Determine if this is the correct root by seeing which side of the
		// radial (0-->T) A' lies upon.
		// If we exchanged discs, invert this test.
		boolean switchRoots = (MyMath.sideOfLine(Point.ZERO, tangent,
				aTranslated) < 0);
		switchRoots ^= exchangeDiscs;

		if (switchRoots)
			tangent.setTo(roots[1], (rSquared - (aTranslated.x * roots[1]))
					/ aTranslated.y);

		// Figure out the translation to apply to the bitangent to undo
		// the effect of shrinking the discs initially
		Point radialAdjust;
		if (r == 0) {
			// Both discs were the same size, and shrunk to points;
			// rotate the A vector 90 degrees CCW and scale appropriately to
			// find tangent point
			float rScale = db.getRadius() / aDistance;
			radialAdjust = new Point(aTranslated.y * rScale, -aTranslated.x
					* rScale);
		} else {
			float rScale = da.getRadius() / r;
			radialAdjust = new Point(rScale * tangent.x, rScale * tangent.y);
		}

		Point bTangentPoint = new Point(bOrigin);
		bTangentPoint.add(tangent);
		bTangentPoint.add(radialAdjust);
		Point aTangentPoint = new Point(aOrigin);
		aTangentPoint.add(radialAdjust);

		bitangent = new Point[2];
		if (!exchangeDiscs) {
			bitangent[0] = bTangentPoint;
			bitangent[1] = aTangentPoint;
		} else {
			bitangent[1] = bTangentPoint;
			bitangent[0] = aTangentPoint;
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
	private Point[] calcBitangent2(Disc da, Disc db) {

		AlgorithmStepper s = mStepper;

		Point[] bitangent = null;

		boolean exchangeDiscs = false;

		if (s.step())
			s.show("Calc bitangents");

		// Proceed assuming db is the larger of the two
		exchangeDiscs = (db.getRadius() < da.getRadius());

		if (exchangeDiscs) {
			Disc tmp = da;
			da = db;
			db = tmp;
		}

		Point aOrigin = da.getOrigin();
		Point bOrigin = db.getOrigin();

		float abDist = MyMath.distanceBetween(aOrigin, bOrigin);
		// If disc b contains disc a, there is no bitangent.
		if (abDist + da.getRadius() <= db.getRadius())
			return null;

		float theta = MyMath.polarAngle(aOrigin.x - bOrigin.x, aOrigin.y
				- bOrigin.y);
		float phi = MyMath.M_DEG * 90
				- (float) Math.asin((db.getRadius() - da.getRadius()) / abDist);
		float alpha;
		if (!exchangeDiscs) {
			alpha = theta + phi;
		} else {
			alpha = theta - phi;
		}
		Point aTangentPoint, bTangentPoint;
		aTangentPoint = MyMath.pointOnCircle(aOrigin, alpha, da.getRadius());
		bTangentPoint = MyMath.pointOnCircle(bOrigin, alpha, db.getRadius());

		bitangent = new Point[2];
		if (!exchangeDiscs) {
			bitangent[0] = bTangentPoint;
			bitangent[1] = aTangentPoint;
		} else {
			bitangent[1] = bTangentPoint;
			bitangent[0] = aTangentPoint;
		}

		return bitangent;
	}

	private Disc[] mDiscs;
	/* private */AlgorithmOptions mOptions;
	private AlgorithmStepper mStepper;
}
