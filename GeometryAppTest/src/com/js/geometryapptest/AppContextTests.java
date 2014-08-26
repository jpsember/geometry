package com.js.geometryapptest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import com.js.basic.Files;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;
import android.test.ServiceTestCase;
import static com.js.basic.Tools.*;

public class AppContextTests extends AndroidTestCase {

	/**
	 * From
	 * http://stackoverflow.com/questions/8605611/get-context-of-test-project
	 * -in-android-junit-test-case
	 * 
	 * @return The {@link Context} of the test project.
	 */
	private Context getTestContext() {
		try {
			Method getTestContext = ServiceTestCase.class
					.getMethod("getTestContext");
			return (Context) getTestContext.invoke(this);
		} catch (final Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public void testAssetsAvailable() throws IOException {

		Context c = getTestContext();
		assertNotNull("context is null", c);

		AssetManager m = c.getAssets();
		assertNotNull("asset manager null", m);

		if (false) {
			String[] assetList = m.list("");
			pr("Asset list:");
			for (String s : assetList) {
				pr(" " + s);
			}
		}

		InputStream is = m.open("snapshots/example.txt");
		String content = Files.readTextFile(is);
		is.close();
		assertTrue(content.indexOf("is an example") >= 0);

	}
}
