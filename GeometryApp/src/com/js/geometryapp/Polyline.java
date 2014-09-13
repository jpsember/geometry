package com.js.geometryapp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.graphics.Color;

import com.js.geometry.Point;
import static com.js.basic.Tools.*;

public class Polyline {

	public Polyline() {
		mBlue = 1.0f;
		setLineWidth(1.0f);
		doNothing();
	}

	/**
	 * Set color for subsequent vertices
	 * 
	 * @param r
	 * @param g
	 * @param b
	 */
	public void setColor(int color) {
		mRed = Color.red(color) / 255.0f;
		mGreen = Color.green(color) / 255.0f;
		mBlue = Color.blue(color) / 255.0f;
		// mAlpha = Color.alpha(color) / 255.0f;
	}

	public void setLineWidth(float lineWidth) {
		mLineWidth = lineWidth;
	}

	public float lineWidth() {
		return mLineWidth;
	}

	public void add(Point vertexLocation) {
		mArray.add((float) vertexLocation.x);
		mArray.add((float) vertexLocation.y);
		mArray.add(mRed);
		mArray.add(mGreen);
		mArray.add(mBlue);
		// TODO: add this later; mArray.add(mAlpha);
		mBuffer = null;
	}

	/**
	 * Make this polyline a closed loop
	 */
	public void close() {
		mClosed = true;
	}

	public boolean isClosed() {
		return mClosed;
	}

	public FloatBuffer asFloatBuffer() {
		if (mBuffer == null) {
			mBuffer = ByteBuffer
					.allocateDirect(mArray.size() * Mesh.BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			mBuffer.put(mArray.array(), 0, mArray.size());
		}
		return mBuffer;
	}

	public int vertexCount() {
		return mArray.size() / 5; // TODO: adjust once alpha channel added
	}

	private float mRed, mGreen, mBlue; // , mAlpha;
	private FloatArray mArray = new FloatArray();
	private FloatBuffer mBuffer;
	private boolean mClosed;
	private float mLineWidth;
}
