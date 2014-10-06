package com.js.geometry;

import static com.js.basic.Tools.*;

/**
 * Utility class for detecting infinite loops (for development purposes only)
 */
public class InfiniteLoopDetector {

	private static final int DEFAULT_ITERATIONS = 2000;

	/**
	 * Create an infinite loop counter with an empty message and a maximum of
	 * 2000 iterations
	 */
	public static void reset() {
		reset(stackTrace(), DEFAULT_ITERATIONS);
	}

	public static void reset(String description, int limit) {
		if (description == null)
			description = stackTrace();
		synchronized (sLock) {
			sLoopDescription = description;
			sMaxIterations = limit;
			sIterationCount = 0;
		}
	}

	public static void update() {
		synchronized (sLock) {
			displayUsageWarning();
			sIterationCount += 1;
			if (sIterationCount >= sMaxIterations) {
				throw new Exception("Infinite loop detected ("
						+ sIterationCount + "): " + sLoopDescription + "\n"
						+ stackTrace(1, 1, null));
			}
		}
	}

	private static void displayUsageWarning() {
		if (!sWarning) {
			sWarning = true;
			warning("Infinite loop detector active (" + stackTrace() + ")");
		}
	}

	private static Object sLock = new Object();
	private static boolean sWarning;
	private static String sLoopDescription;
	private static int sMaxIterations;
	private static int sIterationCount;

	public static class Exception extends RuntimeException {
		public Exception(String message) {
			super(message);
		}
	}
}
