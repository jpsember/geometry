package com.js.geometryapptest;

import java.io.IOException;
import java.io.InputStream;

import com.js.basic.Files;
import com.js.testUtils.MyTestCase;

import android.content.res.AssetManager;

public class AppContextTests extends MyTestCase {

	public void testAssetsAvailable() throws IOException {

		assertNotNull("context is null", getContext());

		AssetManager m = getContext().getAssets();
		assertNotNull("asset manager null", m);

		InputStream is = m.open("snapshots/RandomString.txt");
		String content = Files.readString(is);
		assertTrue("unexpected file contents:\n" + content,
				content.indexOf("jgpgjbyygn") >= 0);
	}
}
