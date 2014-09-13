package com.js.geometryapp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.js.geometry.Point;
import static com.js.basic.Tools.*;

public class Mesh {
	public static final int BYTES_PER_FLOAT = 4;
	public static final int POSITION_COMPONENT_COUNT = 2; // x y
	public static final int COLOR_COMPONENT_COUNT = 3; // r g b

	public Mesh() {
		doNothing();
		setColor(.3f, .3f, .9f);
	}

	/**
	 * Set color for subsequent vertices
	 * 
	 * @param r
	 * @param g
	 * @param b
	 */
	public void setColor(float r, float g, float b) {
		mRed = r;
		mGreen = g;
		mBlue = b;
	}

	public void add(Point vertexLocation) {
		mArray.add((float) vertexLocation.x);
		mArray.add((float) vertexLocation.y);
		mArray.add(mRed);
		mArray.add(mGreen);
		mArray.add(mBlue);
		mBuffer = null;
	}

	public FloatBuffer asFloatBuffer() {
		if (mBuffer == null) {
			mBuffer = ByteBuffer
					.allocateDirect(mArray.size() * BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			mBuffer.put(mArray.array(), 0, mArray.size());
		}
		return mBuffer;
	}

	public int vertexCount() {
		return mArray.size() / 5;
	}

	private float mRed, mGreen, mBlue;
	private FloatArray mArray = new FloatArray();
	private FloatBuffer mBuffer;

}
