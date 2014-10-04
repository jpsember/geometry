package com.js.geometry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import com.js.geometryapp.OurGLTools;

/**
 * A dynamic array of floats
 */
public class FloatArray {

	/**
	 * Clear the array
	 */
	public void clear() {
		mSize = 0;
	}

	/**
	 * Get the size of the array
	 */
	public int size() {
		return mSize;
	}

	/**
	 * Add a value
	 */
	public void add(float f) {
		growTo(mSize + 1);
		mArray[mSize] = f;
		mSize += 1;
	}

	/**
	 * Add a Point as two consecutive floats
	 */
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

	/**
	 * Get a FloatBuffer containing this array's elements
	 */
	public FloatBuffer asFloatBuffer() {
		FloatBuffer mBuffer;
		mBuffer = ByteBuffer.allocateDirect(mSize * OurGLTools.BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBuffer.put(mArray, 0, mSize);
		return mBuffer;
	}

	/**
	 * Add values from a float[]
	 * 
	 * @param array
	 *            source array
	 * @param offset
	 *            index of first element to add
	 * @param count
	 *            number of elements to add
	 */
	public void add(float[] array, int offset, int count) {
		for (int j = 0; j < count; j++)
			add(array[offset + j]);
	}

	/**
	 * Get element
	 * 
	 * @param index
	 */
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
