package com.js.testUtils;

import static com.js.basic.Tools.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.*;

import com.js.basic.Files;

/**
 * Designed to be called from within a unit test. Captures System.out and
 * System.err, and verifies that they match a previously saved 'snapshot' of
 * these streams. If no snapshot exists, creates one.
 * 
 * The methods are static methods, since only a single IOSnapshot object should
 * be in existence at a time.
 */
public class IOSnapshot {

	/**
	 * Open the snapshot; start capturing output
	 */
	public static void open() {
		open(false);
	}

	/**
	 * Open the snapshot; start capturing output
	 * 
	 * @param alwaysReplaceExisting
	 *            if true, and the new snapshot differs from the previous one, a
	 *            warning is printed but no exception is thrown. This is useful
	 *            when writing or debugging code.
	 */
	public static void open(boolean alwaysReplaceExisting) {
		singleton = new IOSnapshot();
		singleton.doOpen(alwaysReplaceExisting);
	}

	/**
	 * Close the snapshot; stop capturing output, and verify with previously
	 * saved output
	 */
	public static void close() {
		if (singleton != null) {
			singleton.doClose();
			singleton = null;
		}
	}

	/**
	 * Abandon any snapshot that is open
	 */
	public static void abandon() {
		if (singleton != null) {
			singleton.doAbandon();
			singleton = null;
		}
	}

	private static IOSnapshot singleton;

	// Users can't construct objects of this class
	private IOSnapshot() {
	}

	private String constructDiff(String s1, String s2) {
		String diff = null;
		try {
			File t1 = File.createTempFile("temp_s1", ".txt");
			File t2 = File.createTempFile("temp_s2", ".txt");
			Files.writeTextFile(t1, s1);
			Files.writeTextFile(t2, s2);

			String[] output = systemCommand("diff " + t1.getPath() + " "
					+ t2.getPath());
			diff = output[0];
			t1.delete();
			t2.delete();
		} catch (Throwable e) {
			diff = "UNABLE TO CONSTRUCT DIFF: " + e;
		}
		return diff;
	}

	private void disconnect() {
		System.setOut(originalStdOut);
		System.setErr(originalStdErr);
	}

	private void doAbandon() {
		disconnect();
	}

	private void doClose() {
		disconnect();
		setSanitizeLineNumbers(false);
		String content = capturedStdOut.content();
		String content2 = capturedStdErr.content();
		if (content2.length() > 0)
			content = content + "\n*** System.err:\n" + content2;

		try {
			boolean write = true;
			if (snapshotPath.exists()) {
				String previousContent = Files.readTextFile(snapshotPath);
				String diff = constructDiff(previousContent, content);
				if (diff == null) {
					write = false;
				} else {
					if (alwaysReplaceExisting)
						pr("...replacing old snapshot content (" + snapshotPath
								+ ")");
					else
						die("Output disagrees with snapshot (" + snapshotPath
								+ "):\n" + diff);
				}
			}
			if (write) {
				System.out.println("...writing new snapshot: " + snapshotPath);
				Files.writeTextFile(snapshotPath, content);
			}
		} catch (IOException e) {
			die(e);
		}
	}

	private void doOpen(boolean alwaysReplaceExisting) {
		this.alwaysReplaceExisting = alwaysReplaceExisting;
		if (alwaysReplaceExisting)
			warning("always replacing existing snapshot", 2);

		calculatePath();
		interceptOutput();
	}

	private void interceptOutput() {
		capturedStdOut = StringPrintStream.build();
		originalStdOut = System.out;
		System.setOut(capturedStdOut);

		capturedStdErr = StringPrintStream.build();
		originalStdErr = System.err;
		System.setErr(capturedStdErr);

		setSanitizeLineNumbers(true);
	}

	private static File determineSnapshotDirectory() {
		if (snapshotDirectory == null) {
			String userDir = System.getProperty("user.dir");
			File d = new File(new File(userDir), "snapshots");
			if (!d.isDirectory())
				die("cannot find directory: " + d);
			snapshotDirectory = d;
		}
		return snapshotDirectory;
	}

	/**
	 * Examine stack trace to find the name of the calling unit test, and
	 * extract the test name from it
	 * 
	 * @return name of method, without the 'test' prefix
	 */
	private String determineTestName() {
		String st = stackTrace(2, 5);

		// Look for first occurrence of '.testXXX:'
		Pattern p = Pattern.compile("\\.test(\\w+):");
		Matcher m = p.matcher(st);
		if (!m.find())
			die("no 'test' method name found in stack trace:\n" + st);
		String matchName = m.group(1);
		return matchName;
	}

	private void calculatePath() {
		File snapshotDir = determineSnapshotDirectory();
		String testName = determineTestName();
		this.snapshotPath = new File(snapshotDir, testName + ".txt");
	}

	private static File snapshotDirectory;
	private File snapshotPath;
	private StringPrintStream capturedStdOut, capturedStdErr;
	private PrintStream originalStdOut, originalStdErr;
	private boolean alwaysReplaceExisting;
}
