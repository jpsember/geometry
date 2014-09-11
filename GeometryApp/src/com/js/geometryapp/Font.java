package com.js.geometryapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;

import com.js.geometry.IPoint;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import static com.js.basic.Tools.*;

public class Font {

	private static final int PRINTABLE_START = ' ';
	private static final int PRINTABLE_TOTAL = '~' + 1 - PRINTABLE_START;

	public Font(int size) {
		doNothing();
		generateBitmap(size);
	}

	private void calculateBitmapSize() {
		int width = 64;
		while (true) {
			int nCols = width / mLetterSize.x;
			int nRows = width / mLetterSize.y;
			if (nRows * nCols >= PRINTABLE_TOTAL)
				break;
			width *= 2;
		}
		mBitmapSize = new IPoint(width, width);
	}

	private void calculateLetterSize() {
		mLetterSize = new IPoint(mAdvanceWidthI,
				(int) Math.ceil(-mFontMetrics.top + mFontMetrics.bottom));
	}

	private void generateBitmap(int fontSize) {
		Paint paint = new Paint();

		Typeface tf = Typeface.MONOSPACE;
		paint.setTypeface(tf);
		paint.setTextSize(fontSize);
		mFontMetrics = paint.getFontMetrics();
		float[] advanceWidths = new float[1];
		// Any character should do, since we're using monospace font
		paint.getTextWidths("m", advanceWidths);
		mAdvanceWidthF = advanceWidths[0];
		mAdvanceWidthI = (int) Math.ceil(mAdvanceWidthF);
		calculateLetterSize();
		mLineHeight = mFontMetrics.leading + mFontMetrics.descent
				- mFontMetrics.ascent;
		mBaseLineOffset = mLineHeight;

		if (false) {
			pr("top=" + mFontMetrics.top);
			pr("bottom=" + mFontMetrics.bottom);
			pr("ascent=" + mFontMetrics.ascent);
			pr("baseLineOffset=" + mBaseLineOffset);
		}

		calculateBitmapSize();
		Bitmap sBitmap = Bitmap.createBitmap(mBitmapSize.x, mBitmapSize.y,
				Bitmap.Config.ARGB_8888);

		sBitmap.eraseColor(Color.TRANSPARENT);

		Canvas canvas = new Canvas(sBitmap);

		paint.setAntiAlias(true);
		paint.setARGB(0xff, 0xff, 0xff, 0xff);

		resetCursor();
		for (int i = PRINTABLE_START; i < PRINTABLE_START + PRINTABLE_TOTAL; i++) {
			String text = Character.toString((char) i);
			canvas.drawText(text, mCursor.x, mCursor.y - mFontMetrics.ascent,
					paint);
			advanceCursor();
		}
		mTexture = new GLTexture(sBitmap);

		generateSprites();
	}

	private void resetCursor() {
		mCursor = new IPoint();
	}

	private void advanceCursor() {
		mCursor.x += mLetterSize.x;
		if (mCursor.x > mBitmapSize.x - mLetterSize.x) {
			mCursor.x = 0;
			mCursor.y += mLetterSize.y;
		}
	}

	private void generateSprites() {
		mSprites = new GLSpriteProgram[PRINTABLE_TOTAL];
		resetCursor();
		for (int i = 0; i < PRINTABLE_TOTAL; i++) {
			Rect bounds = new Rect(mCursor.x, mCursor.y, mLetterSize.x,
					mLetterSize.y);
			GLSpriteProgram s = new GLSpriteProgram(mTexture, bounds);
			mSprites[i] = s;
			advanceCursor();
		}
	}

	public void render(String text, Point location) {
		Point loc = new Point(location.x, location.y - mBaseLineOffset);
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			int j = c - PRINTABLE_START;
			if (j >= 0 && j < PRINTABLE_TOTAL) {
				GLSpriteProgram sp = mSprites[j];
				sp.setPosition(loc);
				sp.render();
			}
			if (c == '\n') {
				loc.x = location.x;
				loc.y -= mBaseLineOffset;
			} else {
				loc.x += mAdvanceWidthI;
			}
		}
	}

	public float lineHeight() {
		return mLineHeight;
	}

	private IPoint mCursor;
	private GLTexture mTexture;
	private FontMetrics mFontMetrics;
	private float mAdvanceWidthF;
	private int mAdvanceWidthI;
	private IPoint mLetterSize;
	private IPoint mBitmapSize;
	private float mBaseLineOffset;
	private float mLineHeight;
	private GLSpriteProgram mSprites[];
}
