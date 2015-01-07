package com.js.geometryapptest;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.js.testUtils.MyTestCase;

import android.content.res.AssetManager;

public class AppContextTests extends MyTestCase {

	public void testAssetsAvailable() throws IOException {

		assertNotNull("context is null", getContext());

		AssetManager m = getContext().getAssets();
		assertNotNull("asset manager null", m);

		InputStream is = m.open("snapshots/RandomString.txt");
		String content = IOUtils.toString(is, "UTF-8");
		is.close();
		assertTrue("unexpected file contents:\n" + content,
				content.indexOf("jgpgjbyygn") >= 0);
	}
}
