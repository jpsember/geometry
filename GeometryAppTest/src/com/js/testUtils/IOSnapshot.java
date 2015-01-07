package com.js.testUtils;

import static com.js.basic.Tools.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.test.AndroidTestCase;

/**
 * Designed to be called from within a unit test. Captures System.out and
 * System.err, and verifies that they match a previously saved 'snapshot' of
 * these streams. If no snapshot exists, creates one.
 * 
 * The methods are static methods, since only a single IOSnapshot object should
 * be in existence at a time.
 */
public class IOSnapshot {

	protected static void prepareContext(AndroidTestCase c) {
		mAndroidTestCase = c;
	}

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

	// Users can't construct objects of this class
	private IOSnapshot() {
	}

	private String constructDiff(String s1, String s2) {
		String diff = null;
		if (!s1.equals(s2))
			diff = "s1:\n" + s1 + "\n\ns2:\n" + s2;
		return diff;
	}

	private void disconnect() {
		System.setOut(originalStdOut);
		System.setErr(originalStdErr);
	}

	private void doAbandon() {
		disconnect();
	}

	private void prepareDynamicSnapshotsDir() {
		if (mDynamicSnapshotsDir == null) {
			File directory = Environment.getExternalStorageDirectory();
			if (directory == null)
				die("external files dir is null");
			mDynamicSnapshotsDir = new File(directory, getDynamicFolderName());

			// Purge any existing snapshots directory
			if (mDynamicSnapshotsDir.exists()) {
				try {
					FileUtils.deleteDirectory(mDynamicSnapshotsDir);
				} catch (IOException e) {
					// It's having problems on my Nexus S; maybe this is a
					// device issue, unrelated to usage of FileUtils?
						warning("Unable to delete snapshots dir: "
								+ mDynamicSnapshotsDir + "\n  exception: " + e);
						die(e);
				}
			}

			// Construct a new one
			mDynamicSnapshotsDir.mkdirs();
			if (!mDynamicSnapshotsDir.exists())
				die("unable to create " + mDynamicSnapshotsDir);
		}
	}

	private File getDynamicFile() {
		if (mDynamicFile == null) {
			prepareDynamicSnapshotsDir();
			mDynamicFile = new File(mDynamicSnapshotsDir, mSnapshotName);
		}
		return mDynamicFile;
	}

	private Context getContext() {
		if (mAndroidTestCase == null)
			die("no AndroidTestCase defined");

		Context c = mAndroidTestCase.getContext();
		if (c == null)
			die("context is null");
		return c;
	}

	private String getDynamicFolderName() {
		String appDataDir = getContext().getApplicationInfo().dataDir;

		if (appDataDir == null)
			die("dataDir is null");

		// Look for suffix '.XXXXtest'
		Pattern p = Pattern.compile("\\.(\\w+test)$");
		Matcher m = p.matcher(appDataDir);
		if (!m.find())
			die("can't extract application name from '" + appDataDir + "'");
		return "assets_" + m.group(1) + "/snapshots";
	}

	private String readStaticContent() {
		String content = null;

		Context c = getContext();

		AssetManager m = c.getAssets();
		if (m == null)
			die("manager is null");

		try {
			InputStream is = m.open("snapshots/" + mSnapshotName);
			content = IOUtils.toString(is, "UTF-8");
			is.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			die(e);
		}
		return content;
	}

	/**
	 * Read expected snapshot; uses static version, if it exists; else returns
	 * null
	 * 
	 * @return
	 */
	private String readReference() {
		String content = readStaticContent();
		return content;
	}

	private void doClose() {
		disconnect();
		setSanitizeLineNumbers(false);
		String content = capturedStdOut.content();
		String content2 = capturedStdErr.content();
		if (content2.length() > 0)
			content = content + "\n*** System.err:\n" + content2;

		boolean write = true;
		String previousContent = readReference();

		if (previousContent != null) {
			String diff = constructDiff(previousContent, content);
			if (diff == null) {
				write = false;
			} else {
				if (alwaysReplaceExisting)
					pr("...replacing old snapshot content (" + mSnapshotName
							+ ")");
				else
					die("Output disagrees with snapshot (" + mSnapshotName
							+ "):\n" + diff);
			}
		}

		try {
			if (write) {
				File f = getDynamicFile();
				System.out.println("...writing new snapshot: " + f);
				FileUtils.write(f, content);
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

	/**
	 * Examine stack trace to find the name of the calling unit test, and
	 * extract the test name from it
	 * 
	 * @return name of method, without the 'test' prefix
	 */
	private String determineTestName() {
		String st = stackTrace(2, 5, null);

		// Look for first occurrence of '.testXXX:'
		Pattern p = Pattern.compile("\\.test(\\w+):");
		Matcher m = p.matcher(st);
		if (!m.find())
			die("no 'test' method name found in stack trace:\n" + st);
		String matchName = m.group(1);
		return matchName;
	}

	private void calculatePath() {
		String testName = determineTestName();
		this.mSnapshotName = testName + ".txt";
	}

	private static IOSnapshot singleton;
	private static AndroidTestCase mAndroidTestCase;
	private static File mDynamicSnapshotsDir;

	private File mDynamicFile;
	private String mSnapshotName;
	private StringPrintStream capturedStdOut, capturedStdErr;
	private PrintStream originalStdOut, originalStdErr;
	private boolean alwaysReplaceExisting;
}
