package com.js.android;

import android.os.Handler;

public class QuiescentDelayOperation {

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
			if (replace)
				existing.mOperation = null;
		}
		return replace;
	}

	public QuiescentDelayOperation(long delayInMS, Runnable operation) {
		mActivationDelay = delayInMS;
		mActivationTime = System.currentTimeMillis() + delayInMS;
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mOperation != null)
					mOperation.run();
			}
		}, delayInMS);
	}

	// Delay in ms before operation is to occur
	private long mActivationDelay;
	// Approximate time operation will occur at
	private long mActivationTime;
	// The operation to perform
	private Runnable mOperation;
}
