package com.js.android;

import android.os.Handler;
import static com.js.basic.Tools.*;

/**
 * Manages an operation to be performed on the UI thread after some amount of
 * inactivity. Allows previously scheduled operations to be delayed further, if
 * activity occurs in the meantime.
 */
public class QuiescentDelayOperation {

	private static final boolean DIAGNOSTIC_PRINTING = false && DEBUG_ONLY_FEATURES;

	/**
	 * Cancel an existing pending operation, if one exists
	 */
	public static void cancelExisting(QuiescentDelayOperation existing) {
		if (existing != null)
			existing.mOperation = null;
	}

	/**
	 * Determine whether an existing pending operation should be replaced.
	 * Should be called when an event needs to occur after x more seconds of
	 * inactivity. Inactivity is defined as the amount of time between calls to
	 * this method.
	 * 
	 * A previous event can be cancelled if it is scheduled for signicantly less
	 * than x seconds in the future.
	 * 
	 * @param existing
	 *            existing operation, or null
	 * @return true if no existing operation exists, or if its delay is not long
	 *         enough; if true, user should construct a new operation
	 */
	public static boolean replaceExisting(QuiescentDelayOperation existing) {
		boolean replace = true;
		if (existing != null) {
			long currentTime = System.currentTimeMillis();
			if (existing.mActivationTime >= currentTime
					+ existing.mActivationDelay / 2)
				replace = false;
			if (replace) {
				cancelExisting(existing);
			}
		}
		return replace;
	}

	/**
	 * Constructor
	 * 
	 * @param debugName
	 *            for diagnostic purposes only
	 * @param delayInSeconds
	 *            amount of 'quiet' time that must elapse before operation is
	 *            performed
	 * @param operation
	 */
	public QuiescentDelayOperation(String debugName, float delayInSeconds,
			Runnable operation) {
		final boolean db = DIAGNOSTIC_PRINTING;

		mActivationDelay = (long) (delayInSeconds * 1000);
		mActivationTime = System.currentTimeMillis() + mActivationDelay;
		mOperation = operation;
		mDebugName = debugName;
		sHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mOperation != null) {
					if (db)
						pr("Time "
								+ d(relativeTime(System.currentTimeMillis()))
								+ ", activating quiescent operation "
								+ mDebugName + ": " + nameOf(mOperation));
					mOperation.run();
				}
			}
		}, mActivationDelay);
		if (db)
			pr("Just created " + this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("QOper ");
		sb.append(mDebugName);
		sb.append(" activationTime " + d(relativeTime(mActivationTime)));
		sb.append(" oper=" + nameOf(mOperation));
		return sb.toString();
	}

	private static long sBaseTime;

	private static int relativeTime(long time) {
		if (DIAGNOSTIC_PRINTING) {
			if (sBaseTime == 0)
				sBaseTime = System.currentTimeMillis();
			return (int) (time - sBaseTime);
		}
		throw new UnsupportedOperationException();
	}

	private static Handler sHandler = new Handler();

	// Delay in ms before operation is to occur
	private long mActivationDelay;
	// Approximate time operation will occur at
	private long mActivationTime;
	// The operation to perform
	private Runnable mOperation;
	private String mDebugName;
}
