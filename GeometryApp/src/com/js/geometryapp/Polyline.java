package com.js.geometryapp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.graphics.Color;

import com.js.geometry.Point;
import com.js.geometry.R;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static com.js.basic.Tools.*;

public class Polyline {

	private static final int POSITION_COMPONENT_COUNT = 2; // x y

	public Polyline() {
		mColor = Color.BLUE;
		setLineWidth(1.0f);
		doNothing();
	}

	/**
	 * Set color for polyline
	 */
	public void setColor(int color) {
		mColor = color;
	}

	public void setLineWidth(float lineWidth) {
		mLineWidth = lineWidth;
	}

	public float lineWidth() {
		return mLineWidth;
	}

	public void add(Point vertexLocation) {
		mArray.add(vertexLocation);
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

	private FloatBuffer asFloatBuffer() {
		if (mBuffer == null) {
			mBuffer = ByteBuffer
					.allocateDirect(mArray.size() * OurGLTools.BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			mBuffer.put(mArray.array(), 0, mArray.size());
		}
		return mBuffer;
	}

	public int vertexCount() {
		return mArray.size() / POSITION_COMPONENT_COUNT;
	}

	public static void prepareRenderer(OurGLRenderer renderer,
			String transformName) {
		GLShader vertexShader = GLShader.readVertexShader(renderer.context(),
				R.raw.polyline_vertex_shader);
		GLShader fragmentShader = GLShader.readFragmentShader(
				renderer.context(), R.raw.polyline_fragment_shader);
		sProgram = new GLProgram(renderer, vertexShader, fragmentShader);
		sProgram.setTransformName(transformName);
		prepareAttributes();
	}

	private static void prepareAttributes() {
		OurGLTools.setProgram(sProgram.getId());
		sPositionLocation = OurGLTools.getProgramLocation("a_Position");
		sColorLocation = OurGLTools.getProgramLocation("u_InputColor");
		sMatrixLocation = OurGLTools.getProgramLocation("u_Matrix");
	}

	public void render() {
		glUseProgram(sProgram.getId());
		sProgram.prepareMatrix(null, sMatrixLocation);

		OurGLTools.convertColorToOpenGL(mColor, sColor);

		glUniform4fv(sColorLocation, 1, sColor, 0);

		FloatBuffer fb = asFloatBuffer();
		fb.position(0);
		int stride = (POSITION_COMPONENT_COUNT) * OurGLTools.BYTES_PER_FLOAT;

		glVertexAttribPointer(sPositionLocation, POSITION_COMPONENT_COUNT,
				GL_FLOAT, false, stride, fb);
		glEnableVertexAttribArray(sPositionLocation);

		// Until issue #18 is fixed, bump up line widths using this hack
		glLineWidth(lineWidth() * 1.5f);

		glDrawArrays(isClosed() ? GL_LINE_LOOP : GL_LINE_STRIP, 0,
				vertexCount());
	}

	private static GLProgram sProgram;
	private static int sPositionLocation;
	private static int sColorLocation;
	private static int sMatrixLocation;
	private static float[] sColor = new float[4];

	private int mColor;
	private FloatArray mArray = new FloatArray();
	private FloatBuffer mBuffer;
	private boolean mClosed;
	private float mLineWidth;
}
