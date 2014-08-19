package com.js.testUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;
import static com.js.basic.Tools.*;

public class MyTest extends TestCase {

	public static void assertEqualsFloat(double expected, double got) {
		assertEquals(expected, got, 1e-10);
	}

	public static void assertStringsMatch(Object s1, Object s2) {
		if (s1 == null)
			s1 = "<null>";
		if (s2 == null)
			s2 = "<null>";
		assertEquals(s1.toString(), s2.toString());
	}

	/**
	 * Fail test since an exception was expected
	 */
	protected void failMissingException() {
		IOSnapshot.abandon();
		fail("expected an exception to be thrown");
	}
	
	@Override
	protected void setUp() {
		tempDirectory = null;
		random = null;
	}

	@Override
	protected void tearDown() {
		// Abandon any snapshot that may not have been closed
		IOSnapshot.abandon();

		// Remove our reference to the temporary directory, so a new one is
		// created for the next test.
		// In order to delete it, if it wasn't empty, we'd have to recursively
		// delete all of its contents
		// and subdirectories; let's instead let the OS take care of this in its
		// own time, since it was created
		// in the OS's temporary directory folder.
		tempDirectory = null;
	}

	/**
	 * Create a temporary directory Note: the web says this code may include
	 * race conditions and security problems; but if we're only using it for
	 * testing, I'm not concerned.
	 * 
	 * @return
	 */
	public File tempDirectory() {
		if (tempDirectory == null) {
			File t = null;
			try {
				t = File.createTempFile("_test_temp_",
						Long.toString(System.nanoTime()));
			} catch (IOException e) {
				die(e);
			}
			if (!(t.delete()))
				die("could not delete temp dir prior to creation");
			if (!t.mkdir())
				die("could not create temp dir");
			tempDirectory = t;
		}
		return tempDirectory;
	}

	public Random random() {
		if (random == null) {
			resetSeed(1942);
		}
		return random;
	}

	public Random resetSeed(int seed) {
		random = new Random(seed);
		return random;
	}

	private Random random;
	private File tempDirectory;
}
