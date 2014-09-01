package com.js.geometryapp;

import java.util.Arrays;
import static com.js.basic.Tools.*;


public class FloatArray {

	public void add(float f) {
		// pr("add " + f + " (size=" + mSize + ")");
		growTo(mSize + 1);
		mArray[mSize] = f;
		mSize += 1;
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
		if (false)
			pr("ignore unused warning");
		return mSize;
	}

	private int mSize;
	private float[] mArray = new float[16];
}
