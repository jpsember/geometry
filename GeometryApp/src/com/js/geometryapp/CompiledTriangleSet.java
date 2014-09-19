package com.js.geometryapp;

import java.nio.FloatBuffer;

public class CompiledTriangleSet {
	public CompiledTriangleSet(FloatBuffer buffer, int nVertices) {
		mFloatBuffer = buffer;
		mVertexCount = nVertices;
	}

	public FloatBuffer floatBuffer() {
		return mFloatBuffer;
	}

	public int numVertices() {
		return mVertexCount;
	}

	private FloatBuffer mFloatBuffer;
	private int mVertexCount;
}
