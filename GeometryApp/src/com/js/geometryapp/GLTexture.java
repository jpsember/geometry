package com.js.geometryapp;

import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glTexParameterf;
import static com.js.basic.Tools.*;

import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

/**
 * Encapsulates an OpenGL texture
 */
public class GLTexture {

	public GLTexture(Bitmap bitmap) {
		OurGLRenderer.ensureOpenGLThread();
		initWithBitmap(bitmap);
	}

	private void initWithBitmap(Bitmap bitmap) {

		ASSERT(bitmap != null);

		mWidth = bitmap.getWidth();
		mHeight = bitmap.getHeight();

		select();
		prepareBitmap(bitmap);
		bitmap.recycle();
	}

	/**
	 * Constructor; must be called from OpenGL thread
	 * 
	 * @param context
	 * @param resourceId
	 */
	public GLTexture(Context context, int resourceId) {
		OurGLRenderer.ensureOpenGLThread();

		doNothing();

		InputStream is = context.getResources().openRawResource(resourceId);
		Bitmap bitmap = BitmapFactory.decodeStream(is);
		initWithBitmap(bitmap);
	}

	public static void reportAnyError() {
		int err = glGetError();
		if (err != 0) {
			pr("OpenGL error! " + err + " : " + GLUtils.getEGLErrorString(err));
		}
	}

	private int textureId() {
		if (mTextureId == 0) {
			int sTextures[] = new int[1];
			// Generate one texture pointer...
			glGenTextures(1, sTextures, 0);
			mTextureId = sTextures[0];
		}
		return mTextureId;
	}

	public void select() {
		glBindTexture(GL10.GL_TEXTURE_2D, textureId());
	}

	private void prepareBitmap(Bitmap mBitmap) {

		// Create Nearest Filtered Texture
		glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_NEAREST);
		glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);

		// Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
				GL10.GL_REPEAT);
		glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
				GL10.GL_REPEAT);

		// Use the Android GLUtils to specify a two-dimensional texture image
		// from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);

		reportAnyError();
	}

	public int width() {
		return mWidth;
	}

	public int height() {
		return mHeight;
	}

	private int mTextureId;
	private int mWidth;
	private int mHeight;
}
