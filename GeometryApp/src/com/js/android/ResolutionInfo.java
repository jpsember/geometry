package com.js.android;

import com.js.basic.Point;

import android.util.DisplayMetrics;
import static com.js.basic.Tools.*;

/**
 * Encapsulates some information about converting physical distances to device
 * pixel values, including views that may have nonstandard scaling factors (such
 * as the OpenGL view that renders the algorithm)
 */
public class ResolutionInfo {

	private static final boolean db = false && DEBUG_ONLY_FEATURES;

	public ResolutionInfo(DisplayMetrics displayMetrics) {
		/**
		 * <pre>
		 * 
		 * DisplayMetrics 
		 * ------------------
		 * For reference, my Google Nexus S phone:
		 * 
		 *  dpi 		      234.462   236.279 
		 * 	pixels 	      800.000   480.000 
		 * 	phys sz 		    3.412     2.031
		 * 	densityDPI   240 
		 *  density      1.5
		 * 
		 * My Samsung Tablet:
		 * 
		 *  dpi           188.148    189.023
		 *  pixels       1280.000    800.000
		 *  phys sz         6.803      4.232
		 *  densityDPI   213 
		 *  density      1.3312501
		 * 
		 * </pre>
		 */
		mDisplayMetrics = displayMetrics;
	}

	public DisplayMetrics getDisplayMetrics() {
		return mDisplayMetrics;
	}

	public float density() {
		ASSERT(getDisplayMetrics().density != 0);
		return getDisplayMetrics().density;
	}

	/**
	 * Determine how many pixels in a UI view there are in a distance expressed
	 * in inches (where a UI view is expressed in terms of actual device pixels)
	 */
	public int inchesToPixelsUI(float inches) {
		// Note: we use only the horizontal dpi for this, which may be different
		// than the vertical dpi; on my phone, this is about a 1% difference
		return (int) (inches * getDisplayMetrics().xdpi);
	}

	/**
	 * Determine how many pixels in the algorithm view there are in a distance
	 * expressed in inches (where the algorithm view scale factor has been
	 * previously set via setInchesToAlgorithmPixels)
	 */
	public float inchesToPixelsAlgorithm(float inches) {
		if (mInchesToPixelsAlgorithm == 0)
			throw new IllegalStateException("Algorithm scale factor undefined");
		return (inches * mInchesToPixelsAlgorithm);
	}

	/**
	 * Set the number of algorithm view pixels per inch
	 */
	public void setInchesToPixelsAlgorithm(float s) {
		mInchesToPixelsAlgorithm = s;
		if (db) {
			pr(this);
		}
	}

	@Override
	public String toString() {
		if (!DEBUG_ONLY_FEATURES)
			return super.toString();
		else {
			StringBuilder sb = new StringBuilder("ResolutionInfo\n");
			DisplayMetrics dm = getDisplayMetrics();
			sb.append(" dpi        " + new Point(dm.xdpi, dm.ydpi) + "\n");
			sb.append(" pixels     "
					+ new Point(dm.widthPixels, dm.heightPixels) + "\n");
			Point ps = new Point(dm.widthPixels / dm.xdpi, dm.heightPixels
					/ dm.ydpi);
			sb.append(" phys sz    " + ps + "\n");
			sb.append(" densityDPI   " + dm.densityDpi + "\n");
			sb.append(" density      " + dm.density + "\n");
			sb.append(" inches/pixels (UI)  = " + inchesToPixelsUI(1) + "\n");
			if (mInchesToPixelsAlgorithm != 0) {
				sb.append(" inches/pixels (alg) = "
						+ d(mInchesToPixelsAlgorithm) + "\n");
			}
			return sb.toString();
		}
	}

	private DisplayMetrics mDisplayMetrics;
	private float mInchesToPixelsAlgorithm;
}
