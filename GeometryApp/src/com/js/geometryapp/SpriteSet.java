package com.js.geometryapp;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.opengl.GLSpriteProgram;
import com.js.opengl.GLTexture;
import com.js.opengl.SpriteContext;
import static com.js.basic.Tools.*;

/**
 * Organizes and prepares sprites for rendering. Sprites are drawn using
 * GLSpritePrograms, with tint mode active
 */
public class SpriteSet {

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param spriteResourceIds
	 *            array of resource ids (e.g. raw.crosshair) to compile sprites
	 *            for
	 */
	public SpriteSet(Context context, int[] spriteResourceIds) {
		mContext = context;
		mTintedSpriteContext = new SpriteContext(
				AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC, true);
		constructSpritePrograms(spriteResourceIds);
	}

	/**
	 * Plot a sprite
	 */
	public void plot(int spriteId, Point location, int color) {
		SpriteProgramRecord spriteRec = getProgramForSpriteId(spriteId);
		mTintedSpriteContext.setTintColor(color);
		GLSpriteProgram sprite = spriteRec.spriteProgram();
		sprite.setPosition(MyMath.subtract(location, spriteRec.centerpoint()));
		sprite.render();
	}

	private SpriteProgramRecord getProgramForSpriteId(int spriteId) {
		SpriteProgramRecord rec = mSpriteMap.get(spriteId);
		if (rec == null)
			throw new IllegalArgumentException("no sprite found for id "
					+ spriteId);
		return rec;
	}

	private class SpriteProgramRecord {
		public SpriteProgramRecord(int spriteId) {
			GLTexture t = new GLTexture(mContext, spriteId);
			Rect rect = new Rect(0, 0, t.width(), t.height());

			// Inset rect by 1/2 pixel to avoid edge effects

			// Why does insetting by only .5 leave gray borders? Was this
			// because the texture wasn't a power of 2?
			rect.inset(.5f, .5f);
			mCenterpoint = new Point(rect.width / 2, rect.height / 2);
			mSpriteProgram = new GLSpriteProgram(mTintedSpriteContext, t, rect);
			// TODO: allow more sophisticated method of determining centerpoint
		}

		public GLSpriteProgram spriteProgram() {
			return mSpriteProgram;
		}

		public Point centerpoint() {
			return mCenterpoint;
		}

		private Point mCenterpoint;
		private GLSpriteProgram mSpriteProgram;
	}

	private void constructSpritePrograms(int[] spriteResourceIds) {
		for (int resourceId : spriteResourceIds) {
			SpriteProgramRecord rec = new SpriteProgramRecord(resourceId);
			mSpriteMap.put(resourceId, rec);
		}
	}

	static {
		doNothing();
	}

	private Context mContext;
	private Map<Integer, SpriteProgramRecord> mSpriteMap = new HashMap();
	private SpriteContext mTintedSpriteContext;
}
