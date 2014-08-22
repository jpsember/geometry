package com.js.geometryapptest;

import java.io.File;
import java.io.IOException;

import android.test.AndroidTestCase;

import com.js.basic.Files;

public class MiscAndroidTests extends AndroidTestCase {

	public void testExternalFileDeletedFileIsDeletedImmediately()
			throws IOException {

		File directory = getContext().getExternalFilesDir(null);
		assertNotNull("getExternalFilesDir returned null", directory);

		File sampleFile = new File(directory, "___xyz___.txt");
		Files.writeTextFile(sampleFile, "hello");

		assertTrue(sampleFile.exists());
		assertTrue(sampleFile.delete());
		assertFalse(sampleFile.exists());
	}
}
