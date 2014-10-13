package com.js.opengl;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;

import com.js.android.BitmapUtil;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;

/**
 * Organizes and prepares sprites for rendering. Sprites are drawn using
 * GLSpritePrograms, and at present, tint mode is always active.
 */
public class SpriteSet {

	private static final boolean DUMP_ATLAS_PNG = false;

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param spriteResourceIds
	 *            array of resource ids (e.g. raw.squareicon)
	 */
	public SpriteSet(Context context, int[] spriteResourceIds) {
		mContext = context;
		constructSpritePrograms(spriteResourceIds);
		setAtlasSize(new Point(256, 256));
	}

	/**
	 * Set the transform to be applied to sprites when plotted; by default this
	 * is OurGLRenderer.TRANSFORM_DEVICE_TO_NDC
	 */
	public void setTransformName(String transformName) {
		if (compiled())
			throw new IllegalStateException();
		mTransformName = transformName;
	}

	public void setAtlasSize(Point size) {
		if (compiled())
			throw new IllegalStateException();
		mAtlasSize = size;
	}

	public void compile() {
		if (compiled())
			throw new IllegalStateException();
		if (mTransformName == null)
			mTransformName = OurGLRenderer.TRANSFORM_NAME_DEVICE_TO_NDC;
		mSpriteContext = new SpriteContext(mTransformName, true);
		arrangeSprites();
		GLTexture atlasTexture = buildAtlasTexture();
		buildSpritePrograms(atlasTexture);
	}

	/**
	 * Plot a sprite
	 */
	public void plot(int spriteId, Point location, int color) {
		if (!compiled())
			throw new IllegalStateException("sprite set not yet compiled");

		SpriteProgramRecord spriteRec = getProgramForSpriteId(spriteId);
		mSpriteContext.setTintColor(color);
		GLSpriteProgram sprite = spriteRec.spriteProgram();
		sprite.setPosition(MyMath.subtract(location, spriteRec.centerpoint()));
		sprite.render();
	}

	private void buildSpritePrograms(GLTexture atlasTexture) {
		for (SpriteProgramRecord rec : mSpriteMap.values()) {
			Rect rect = rec.mBounds;
			rec.mCenterpoint = new Point(rect.midX() - rect.x, rect.midY()
					- rect.y);
			// TODO: allow more sophisticated method of determining centerpoint
			rec.mSpriteProgram = new GLSpriteProgram(mSpriteContext,
					atlasTexture, rect);
		}
	}

	private boolean compiled() {
		return mSpriteContext != null;
	}

	private SpriteProgramRecord getProgramForSpriteId(int spriteId) {
		SpriteProgramRecord rec = mSpriteMap.get(spriteId);
		if (rec == null)
			throw new IllegalArgumentException("no sprite found for id "
					+ spriteId);
		return rec;
	}

	private Bitmap buildAtlasBitmap() {
		Bitmap.Config conf = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = Bitmap.createBitmap((int) mAtlasSize.x,
				(int) mAtlasSize.y, conf);
		return bitmap;
	}

	private GLTexture buildAtlasTexture() {
		Bitmap bitmap = buildAtlasBitmap();
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		for (SpriteProgramRecord rec : mSpriteMap.values()) {
			Rect r = rec.mBounds;
			canvas.drawBitmap(rec.mBitmap, r.x, r.y, null);
			// dispose of the bitmap now that it's been plotted to the atlas
			rec.mBitmap = null;
		}
		if (DUMP_ATLAS_PNG) {
			BitmapUtil.saveBitmapAsPNG(bitmap, "atlas");
		}
		GLTexture atlasTexture = new GLTexture(bitmap);
		return atlasTexture;
	}

	private void arrangeSprites() {
		AtlasBuilder b = new AtlasBuilder(mAtlasSize);
		b.setPadding(1);

		for (SpriteProgramRecord rec : mSpriteMap.values()) {
			b.add(rec.mBounds);
		}
		boolean success = b.build();
		if (!success)
			throw new IllegalArgumentException("failed to build atlas (size "
					+ mAtlasSize + ")");
	}

	private void constructSpritePrograms(int[] spriteResourceIds) {
		for (int resourceId : spriteResourceIds) {
			SpriteProgramRecord rec = new SpriteProgramRecord(resourceId);
			if (mSpriteMap.containsKey(resourceId)) {
				throw new IllegalArgumentException(
						"duplicate sprite resource: " + resourceId);
			}
			mSpriteMap.put(resourceId, rec);
		}
	}

	private class SpriteProgramRecord {
		public SpriteProgramRecord(int spriteId) {
			Bitmap bitmap = BitmapUtil.readFromResource(mContext, spriteId);
			bitmap = BitmapUtil.trimPadding(bitmap);
			// Construct bounding rect whose position is undefined; we'll have
			// the AtlasBuilder arrange them all at once later
			mBounds = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
			mBitmap = bitmap;
		}

		public GLSpriteProgram spriteProgram() {
			return mSpriteProgram;
		}

		public Point centerpoint() {
			return mCenterpoint;
		}

		// bounds of this sprite within its atlas
		private Rect mBounds;
		private Bitmap mBitmap;
		private Point mCenterpoint;
		private GLSpriteProgram mSpriteProgram;
	}

	private Context mContext;
	private Map<Integer, SpriteProgramRecord> mSpriteMap = new HashMap();
	private SpriteContext mSpriteContext;
	private String mTransformName;
	private Point mAtlasSize;
}
