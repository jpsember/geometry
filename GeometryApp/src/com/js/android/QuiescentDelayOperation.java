package com.js.android;

import android.os.Handler;
import static com.js.basic.Tools.*;

public class QuiescentDelayOperation {

	/**
	 * Cancel an existing pending operation, if one exists
	 */
	public static void cancelExisting(QuiescentDelayOperation existing) {
		if (existing != null)
			existing.mOperation = null;
	}

	/**
	 * Determine whether an existing pending operation should be replaced
	 * 
	 * @param existing
	 *            existing operation, or null
	 * @return true if no existing operation exists, or if its delay is not long
	 *         enough
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

	public QuiescentDelayOperation(long delayInMS, Runnable operation) {
		mActivationDelay = delayInMS;
		mActivationTime = System.currentTimeMillis() + delayInMS;
		mOperation = operation;
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				pr("Time " + f(relativeTime(System.currentTimeMillis()))
						+ ", activating quiescent operation: "
						+ nameOf(mOperation));
				if (mOperation != null)
					mOperation.run();
			}
		}, delayInMS);
		pr("Just created " + this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("QOper");
		sb.append(" activationTime " + f(relativeTime(mActivationTime)));
		sb.append(" oper=" + nameOf(mOperation));
		return sb.toString();
	}

	private static long sBaseTime;
	static {
		sBaseTime = System.currentTimeMillis();
	}

	private static int relativeTime(long time) {
		return (int) (time - sBaseTime);
	}

	// Delay in ms before operation is to occur
	private long mActivationDelay;
	// Approximate time operation will occur at
	private long mActivationTime;
	// The operation to perform
	private Runnable mOperation;
}
