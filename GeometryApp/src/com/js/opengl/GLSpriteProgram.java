package com.js.opengl;

import java.nio.FloatBuffer;

import com.js.geometry.FloatArray;
import com.js.basic.Point;
import com.js.basic.Rect;

public class GLSpriteProgram {

	/**
	 * Constructor
	 * 
	 * @param texture
	 *            texture containing sprite
	 * @param textureWindow
	 *            subrectangle within texture representing sprite
	 */
	public GLSpriteProgram(SpriteContext spriteContext, GLTexture texture,
			Rect textureWindow) {
		mSpriteContext = spriteContext;
		mTexture = texture;
		mTextureWindow = textureWindow;

		constructVertexInfo();
	}

	private void constructVertexInfo() {
		FloatArray mMesh = new FloatArray();

		float textureWidth = mTextureWindow.width;
		float textureHeight = mTextureWindow.height;

		Point p0 = new Point(0, 0);
		Point p2 = new Point(textureWidth, textureHeight);
		Point p1 = new Point(p2.x, p0.y);
		Point p3 = new Point(p0.x, p2.y);

		Point t0 = new Point(mTextureWindow.x / mTexture.width(),
				mTextureWindow.endY() / mTexture.height());
		Point t2 = new Point(mTextureWindow.endX() / mTexture.width(),
				mTextureWindow.y / mTexture.height());
		Point t1 = new Point(t2.x, t0.y);
		Point t3 = new Point(t0.x, t2.y);

		mMesh.add(p0);
		mMesh.add(t0);
		mMesh.add(p1);
		mMesh.add(t1);
		mMesh.add(p2);
		mMesh.add(t2);

		mMesh.add(p0);
		mMesh.add(t0);
		mMesh.add(p2);
		mMesh.add(t2);
		mMesh.add(p3);
		mMesh.add(t3);

		mVertexData = mMesh.asFloatBuffer();
	}

	/**
	 * Set offset of bottom left corner of sprite within view
	 * 
	 * @param x
	 * @param y
	 */
	public void setPosition(float x, float y) {
		mPosition.x = x;
		mPosition.y = y;
	}

	/**
	 * Set offset of bottom left corner of sprite within view
	 */
	public void setPosition(Point position) {
		setPosition(position.x, position.y);
	}

	/**
	 * Draw the sprite
	 */
	public void render() {
		mSpriteContext.renderSprite(mTexture, mVertexData, mPosition);
	}

	private FloatBuffer mVertexData;
	private GLTexture mTexture;
	private Rect mTextureWindow;
	private Point mPosition = new Point();
	private SpriteContext mSpriteContext;
}
