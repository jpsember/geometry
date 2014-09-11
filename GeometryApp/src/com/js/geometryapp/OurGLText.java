package com.js.geometryapp;

import com.js.geometry.Point;
import com.js.geometry.Rect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import static com.js.basic.Tools.*;

/**
 * Experimental rendering of text with OpenGL
 * 
 */
public class OurGLText {

	public OurGLText(String mText) {
		doNothing();

		Bitmap sBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
		Canvas sCanvas = new Canvas(sBitmap);

		int ts = 64;
		Point position = new Point(16, 112);
		{
			sBitmap.eraseColor(0);

			// Draw the text
			Paint textPaint = new Paint();
			textPaint.setTextSize(ts);
			textPaint.setAntiAlias(true);
			textPaint.setARGB(0xff, 0xf0, 0x00, 0xf0);
			// draw the text centered
			sCanvas.drawText(mText, position.x, position.y, textPaint);
		}
		mTexture = new GLTexture(sBitmap);
		mSprite = new GLSpriteProgram(mTexture, new Rect(position.x, position.y
				- ts, 200, ts));

	}

	public void render(OurGLRenderer renderer) {
		mSprite.setPosition(100, 200);
		mSprite.render();
	}

	private GLTexture mTexture;
	private GLSpriteProgram mSprite;
}
