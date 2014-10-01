package com.js.geometryapp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import com.js.geometry.Point;

public class FloatArray {

	public void clear() {
		mSize = 0;
	}

	public void add(float f) {
		growTo(mSize + 1);
		mArray[mSize] = f;
		mSize += 1;
	}

	public void add(Point point) {
		add(point.x);
		add(point.y);
	}

	/**
	 * Get array containing this FloatArray's values. Note: the array returned
	 * may contain more elements than are in the FloatArray
	 */
	public float[] array(boolean trimToSize) {
		if (!trimToSize || mArray.length == mSize)
			return mArray;
		return Arrays.copyOf(mArray, mSize);
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
		mBuffer = ByteBuffer.allocateDirect(mSize * OurGLTools.BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBuffer.put(mArray, 0, mSize);
		return mBuffer;
	}

	public void add(float[] array, int offset, int count) {
		for (int j = 0; j < count; j++)
			add(array[offset + j]);
	}

	public float get(int index) {
		if (index >= mSize)
			throw new ArrayIndexOutOfBoundsException();
		return mArray[index];
	}

	private int mSize;
	private float[] mArray = new float[16];
}
