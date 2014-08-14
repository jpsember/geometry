package com.js.basicTest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.js.basic.Files;
import com.js.testUtils.*;

public class FilesTest extends MyTest {
	
	private static void ourWriteTextFile(File path, String content) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(path));
		w.write(content);
		w.close();
	}
	
	private File buildFile() {
		return  new File(tempDirectory(),"textfile.txt");
	}
	
	private final static String CONTENT = "Alpha\n   \t Bravo\n\n\nCharlie\n";
	private final static String CONTENT2 = "Epsilon\n";
	
	public void testReadTextFile() throws IOException {
		File path = buildFile();
		ourWriteTextFile(path,CONTENT);
		
		String x = Files.readTextFile(path);
		assertStringsMatch(x,CONTENT);
	}
	
	public void testReadTextFileWithoutFinalLinefeed() throws IOException {
		File path = buildFile();
		ourWriteTextFile(path,CONTENT.substring(0,CONTENT.length()-1));
		
		String x = Files.readTextFile(path);
		assertStringsMatch(x,CONTENT);
	}

	public void testWriteTextFile() throws IOException {
		File path = buildFile();
		Files.writeTextFile(path, CONTENT);
		assertTrue(path.isFile());
	}

	public void testWriteTextFileWhenChanged() throws IOException {
		File path = buildFile();
		Files.writeTextFile(path, CONTENT);
		assertTrue(path.isFile());
		
		Files.writeTextFile(path,CONTENT2);
		assertStringsMatch(CONTENT2,Files.readTextFile(path));
	}
	
	public void testWriteTextFileWhenUnchangedIfNotExplicitlyToldNotTo()
			throws IOException {
		File path = buildFile();
		Files.writeTextFile(path, CONTENT);
		assertTrue(path.isFile());

		long ms = path.lastModified();

		for (int pass = 0; pass < 2; pass++) {
			path.setLastModified(ms - 2000);
			assertTrue(path.lastModified() < ms);

			if (pass == 0)
				Files.writeTextFile(path, CONTENT);
			else
				Files.writeTextFile(path, CONTENT, false);

			assertTrue(path.lastModified() >= ms);
		}
	}

	public void testDoesntWriteTextFileWhenUnchanged() throws IOException {
		File dir = this.tempDirectory();
		File path = new File(dir,"textfile.txt");
		Files.writeTextFile(path, CONTENT);
		assertTrue(path.isFile());
		
		long ms = path.lastModified();
		path.setLastModified(ms - 2000);
		assertTrue(path.lastModified() < ms);
		
		Files.writeTextFile(path,CONTENT,true);
		assertTrue(path.lastModified() < ms);
	}

	public void testDoesWriteTextFileWhenChanged() throws IOException {
		File dir = this.tempDirectory();
		File path = new File(dir,"textfile.txt");
		Files.writeTextFile(path, CONTENT);
		assertTrue(path.isFile());
		
		long ms = path.lastModified();
		path.setLastModified(ms - 2000);
		assertTrue(path.lastModified() < ms);
		
		Files.writeTextFile(path,CONTENT2,true);
		assertTrue(path.lastModified() >= ms);
		
		path.setLastModified(ms - 2000);
		assertTrue(path.lastModified() < ms);
		
		Files.writeTextFile(path,CONTENT);
		assertTrue(path.lastModified() >= ms);
	}

}

