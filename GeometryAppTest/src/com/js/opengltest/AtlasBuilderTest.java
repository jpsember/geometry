package com.js.opengltest;

import com.js.basic.Point;
import com.js.basic.Rect;
import com.js.opengl.AtlasBuilder;
import com.js.testUtils.MyTestCase;
import static com.js.basic.Tools.*;

public class AtlasBuilderTest extends MyTestCase {

	private Rect randomRect() {
		Rect r = new Rect(0, 0, random().nextInt(20) + 4,
				random().nextInt(20) + 4);
		return r;
	}

	private AtlasBuilder mAtlasBuilder;
	private Rect mAtlasBounds;

	private AtlasBuilder builder(int width, int height) {
		if (mAtlasBuilder == null) {
			mAtlasBounds = new Rect(0, 0, width, height);
			mAtlasBuilder = new AtlasBuilder(mAtlasBounds.size());
		}
		return mAtlasBuilder;
	}

	private AtlasBuilder builder() {
		return builder(100, 100);
	}

	private void verifyAllFit() {
		if (db)
			pr("Atlas bounds: " + mAtlasBounds);
		AtlasBuilder b = builder();
		for (int i = 0; i < b.size(); i++) {
			Rect r = b.get(i);
			if (db)
				pr(" Rect " + d(i) + ": " + r);
			assertTrue(mAtlasBounds.contains(r));
			for (int j = 0; j < i; j++)
				assertFalse(r.intersects(b.get(j)));
		}
	}

	public void testAtlasBuildInputTooWide() {
		AtlasBuilder b = builder();
		for (int i = 0; i < 20; i++) {
			Rect r = randomRect();
			if (i == 7)
				r.width = mAtlasBounds.width + 20;
			b.add(r);
		}
		boolean success = b.build();
		assertFalse(success);
	}

	public void testAtlasBuildInputTooTall() {
		AtlasBuilder b = builder();
		for (int i = 0; i < 20; i++) {
			Rect r = randomRect();
			if (i == 7)
				r.height = mAtlasBounds.height + 20;
			b.add(r);
		}
		boolean success = b.build();
		assertFalse(success);
	}

	public void testAtlasBuild() {
		for (int i = 0; i < 20; i++) {
			builder().add(randomRect());
		}
		boolean success = builder().build();
		assertTrue(success);

		verifyAllFit();
	}

	public void testAtlasBuildUniform() {
		for (int i = 0; i < 20; i++) {
			Rect r = new Rect(0, 0, 7, 7);
			builder().add(r);
		}
		boolean success = builder().build();
		assertTrue(success);

		verifyAllFit();
	}

	public void testAtlasBuildUniformWithPadding() {
		builder().setPadding(4);
		for (int i = 0; i < 20; i++) {
			Rect r = new Rect(0, 0, 12, 12);
			builder().add(r);
		}
		boolean success = builder().build();
		assertTrue(success);
		verifyAllFit();
	}

	public void testAtlasBuildInputTooMany() {
		AtlasBuilder b = builder(50, 50);
		for (int i = 0; i < 20; i++) {
			Rect r = randomRect();
			b.add(r);
		}
		boolean success = b.build();
		assertFalse(success);
	}

	public void testAtlasBuildExpandUntilFit() {
		for (int i = 0; i < 20; i++) {
			builder().add(randomRect());
		}
		for (int j = 0;; j++) {
			int size = (j + 3) * (j + 3);
			Point as = new Point(size, size);
			builder().setAtlasSize(as);
			boolean success = builder().build();
			if (size <= 50) {
				assertFalse(success);
			}
			if (success) {
				verifyAllFit();
				break;
			}
			assertTrue(size <= 100);
		}
	}

}
