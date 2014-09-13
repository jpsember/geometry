package com.js.geometryapp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import com.js.geometry.Point;


public class FloatArray {

	public void add(float f) {
		growTo(mSize + 1);
		mArray[mSize] = f;
		mSize += 1;
	}

	public void add(Point point) {
		add(point.x);
		add(point.y);
	}

	public float[] array() {
		return mArray;
	}

	private void growTo(int required) {
		if (capacity() < required) {
			required = Math.max(required, capacity() * 2);
			mArray = Arrays.copyOf(mArray, required);
		}
	}

	private int capacity() {
		return mArray.length;
	}

	public int size() {
		return mSize;
	}

	public FloatBuffer asFloatBuffer() {
		FloatBuffer mBuffer;
		mBuffer = ByteBuffer
				.allocateDirect(mArray.length * Mesh.BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBuffer.put(mArray, 0, mArray.length);
		return mBuffer;
	}

	private int mSize;
	private float[] mArray = new float[16];
}
