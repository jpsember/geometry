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
		growTo(mSize + 2);
		mArray[mSize] = point.x;
		mArray[mSize + 1] = point.y;
		mSize += 2;
	}

	/**
	 * Get the array containing this FloatArray's values
	 * 
	 * @param trimToSize
	 *            if false, the returned array may have extra padding; if true,
	 *            it will not, but may be a copy of the original
	 */
	public float[] array(boolean trimToSize) {
		if (!trimToSize || mArray.length == mSize)
			return mArray;
		return Arrays.copyOf(mArray, mSize);
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

	private void growTo(int required) {
		if (capacity() < required) {
			required = Math.max(required, capacity() * 2);
			mArray = Arrays.copyOf(mArray, required);
		}
	}

	private int capacity() {
		return mArray.length;
	}

	private int mSize;
	private float[] mArray = new float[16];
}
