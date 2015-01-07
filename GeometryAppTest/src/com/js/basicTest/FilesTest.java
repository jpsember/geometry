package com.js.basicTest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.js.basic.Files;

import com.js.testUtils.MyTestCase;

public class FilesTest extends MyTestCase {

	private static void ourWriteTextFile(File path, String content)
			throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(path));
		w.write(content);
		w.close();
	}

	private File buildFile() {
		return new File(tempDirectory(), "textfile.txt");
	}

	private final static String CONTENT = "Alpha\n   \t Bravo\n\n\nCharlie\n";
	private final static String CONTENT2 = "Epsilon\n";

	public void testReadTextFile() throws IOException {
		File path = buildFile();
		ourWriteTextFile(path, CONTENT);

		String x = FileUtils.readFileToString(path);
		assertStringsMatch(x, CONTENT);
	}

	public void testWriteTextFile() throws IOException {
		File path = buildFile();
		FileUtils.write(path, CONTENT);
		assertTrue(path.isFile());
	}

	public void testWriteTextFileWhenChanged() throws IOException {
		File path = buildFile();
		FileUtils.write(path, CONTENT);
		assertTrue(path.isFile());

		FileUtils.write(path, CONTENT2);
		assertStringsMatch(CONTENT2, FileUtils.readFileToString(path));
	}

	public void testDoesntWriteTextFileWhenUnchanged() throws IOException {
		File dir = this.tempDirectory();
		File path = new File(dir, "textfile.txt");
		FileUtils.write(path, CONTENT);
		assertTrue(path.isFile());

		long ms = path.lastModified();
		path.setLastModified(ms - 2000);
		assertTrue(path.lastModified() < ms);

		Files.writeStringToFileIfChanged(path, CONTENT);
		assertTrue(path.lastModified() < ms);
	}

	public void testDoesWriteTextFileWhenChanged() throws IOException {
		File dir = this.tempDirectory();
		File path = new File(dir, "textfile.txt");
		FileUtils.write(path, CONTENT);
		assertTrue(path.isFile());

		long ms = path.lastModified();
		path.setLastModified(ms - 2000);
		assertTrue(path.lastModified() < ms);

		Files.writeStringToFileIfChanged(path, CONTENT2);
		assertTrue(path.lastModified() >= ms);

		path.setLastModified(ms - 2000);
		assertTrue(path.lastModified() < ms);

		FileUtils.write(path, CONTENT);
		assertTrue(path.lastModified() >= ms);
	}

	public void testHasExtension() {

		String[] a = { "alpha.txt",//
				"com/alpha.txt",//
				"!com.txt/alpha",//
				"!", //
				"!Volumes/Macintosh HD",//
		};
		for (String s : a) {
			boolean hasExt = true;
			if (s.startsWith("!")) {
				s = s.substring(1);
				hasExt = false;
			}
			assertEquals(hasExt, Files.hasExtension(new File(s)));
		}
	}

	public void testGetExtension() {
		String[] a = { "alpha.txt", "txt",//
				"com/alpha.txt", "txt",//
				"com.txt/alpha", "",//
				"", "", //
				"Volumes/Macintosh HD", "",//
		};
		for (int i = 0; i < a.length; i += 2) {
			String s = a[i];
			String ext = a[i + 1];
			assertEquals(ext, Files.getExtension(new File(s)));
		}
	}

	public void testRemoveExtension() {
		String[] a = {//
		"", "", //
				"foo.txt", "foo",//
				"a/b/c.jpg", "a/b/c",//
				"a/b/c", "a/b/c",//
				"a.b/c", "a.b/c",//
		};
		for (int i = 0; i < a.length; i += 2) {
			String before = a[i];
			String after = a[i + 1];
			assertEquals(new File(after),
					Files.removeExtension(new File(before)));
		}
	}

	public void testSetExtension() {
		String[] a = {//
		"foo.txt", "bin", "foo.bin", //
				"a/b/c.jpg", "jpg", "a/b/c.jpg",//
				"a/b/c", "jpg", "a/b/c.jpg",//
				"a.b/c", "", "a.b/c",//
				"a.b/c.jpg", "", "a.b/c",//
				"a.b.jpg/c.jpg", "", "a.b.jpg/c",//
		};
		for (int i = 0; i < a.length; i += 3) {
			String before = a[i];
			String ext = a[i + 1];
			String after = a[i + 2];

			assertEquals(new File(after),
					Files.setExtension(new File(before), ext));
		}
	}

	public void testFileEquals() {
		String[] f = {//
		"a.b/c.jpg", "a.b/c.jpg", //
				"a/b/c", "a/b",//
				null, "a/b",//
				null, null,//
		};
		for (int i = 0; i < f.length; i += 2) {
			String s1 = f[i];
			String s2 = f[i + 1];
			File f1 = null;
			File f2 = null;
			if (s1 != null)
				f1 = new File(s1);
			if (s2 != null)
				f2 = new File(s2);

			// assertEquals(equal(f1, f2), equal(s1, s2));
		}

	}

}
