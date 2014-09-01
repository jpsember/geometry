package com.js.geometryapp;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

import java.nio.FloatBuffer;

import android.graphics.Matrix;

public class GLProgram {

	public static GLProgram build(GLShader vertexShader, GLShader fragmentShader) {

		GLProgram p = new GLProgram();
		p.buildAux(vertexShader, fragmentShader);
		return p;
	}

	private void buildAux(GLShader vertexShader, GLShader fragmentShader) {
		mProgramObjectId = glCreateProgram();
		if (mProgramObjectId == 0) {
			die("unable to create program");
		}
		glAttachShader(mProgramObjectId, vertexShader.getId());
		glAttachShader(mProgramObjectId, fragmentShader.getId());
		glLinkProgram(mProgramObjectId);

		glGetProgramiv(mProgramObjectId, GL_LINK_STATUS, sResultCode, 0);
		if (!success()) {
			warning("failed to link program:\n" + glGetProgramInfoLog(getId()));
			return;
		}

		validate();

		prepareAttributes();
	}

	private void validate() {
		glValidateProgram(getId());

		glGetProgramiv(mProgramObjectId, GL_VALIDATE_STATUS, sResultCode, 0);
		if (!success()) {
			warning("failed to validate program:\n"
					+ glGetProgramInfoLog(getId()));
			return;
		}
	}

	public int getId() {
		return mProgramObjectId;
	}

	private void dispose() {
		if (mProgramObjectId != 0) {
			glDeleteProgram(mProgramObjectId);
			mProgramObjectId = 0;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}

	private static boolean success() {
		return sResultCode[0] != 0;
	}

	private void prepareAttributes() {
		// Must agree with simple_vertex_shader.glsl
		mPositionLocation = glGetAttribLocation(mProgramObjectId, "a_Position");
		mColorLocation = glGetAttribLocation(mProgramObjectId, "a_Color");
		mMatrixLocation = glGetUniformLocation(mProgramObjectId, "u_Matrix");
	}

	private static int sResultCode[] = new int[1];

	/**
	 * Convenience methods to calculate index of matrix element from
	 * column-major, 4x4 matrix
	 * 
	 * @param row
	 * @param col
	 * @return
	 */
	private static int i4(int row, int col) {
		return col * 4 + row;
	}

	/**
	 * Convenience methods to calculate index of matrix element from row-major,
	 * 3x3 matrix
	 * 
	 * @param row
	 * @param col
	 * @return
	 */
	private static int i3(int row, int col) {
		return row * 3 + col;
	}

	/**
	 * Render mesh
	 * 
	 * @param mesh
	 * @param projectionMatrix
	 *            2D screen to NDC transformation matrix; it is converted to a
	 *            3D version, suitable for OpenGL rendering
	 */
	public void render(Mesh mesh, Matrix projectionMatrix) {

		// Concatenate the mesh matrix to the projection matrix
		Matrix m;

		// projectionMatrix.getValues(v3);
		{
			// float v3b[] = new float[9];
			m = new Matrix(mesh.getTransformMatrix());
			m.postConcat(projectionMatrix);
		}

		// Transform 2D screen->NDC matrix to a 3D version
		{
			float v3[] = new float[9];
			m.getValues(v3);

			float v4[] = new float[16];

			v4[i4(0, 0)] = v3[i3(0, 0)];
			v4[i4(0, 1)] = v3[i3(0, 1)];
			v4[i4(0, 2)] = 0;
			v4[i4(0, 3)] = v3[i3(0, 2)];

			v4[i4(1, 0)] = v3[i3(1, 0)];
			v4[i4(1, 1)] = v3[i3(1, 1)];
			v4[i4(1, 2)] = 0;
			v4[i4(1, 3)] = v3[i3(1, 2)];

			v4[i4(2, 0)] = 0;
			v4[i4(2, 1)] = 0;
			v4[i4(2, 2)] = 1;
			v4[i4(2, 3)] = 0;

			v4[i4(3, 0)] = v3[i3(2, 0)];
			v4[i4(3, 1)] = v3[i3(2, 1)];
			v4[i4(3, 2)] = 0;
			v4[i4(3, 3)] = v3[i3(2, 2)];

			glUniformMatrix4fv(mMatrixLocation, 1, false, v4, 0);
		}

		// Not sure if this needs to be done every time
		glUseProgram(getId());

		FloatBuffer fb = mesh.asFloatBuffer();
		fb.position(0);
		int stride = (Mesh.POSITION_COMPONENT_COUNT + Mesh.COLOR_COMPONENT_COUNT)
				* Mesh.BYTES_PER_FLOAT;

		glVertexAttribPointer(mPositionLocation, Mesh.POSITION_COMPONENT_COUNT,
				GL_FLOAT, false, stride, fb);
		glEnableVertexAttribArray(mPositionLocation);

		fb.position(Mesh.POSITION_COMPONENT_COUNT);
		glVertexAttribPointer(mColorLocation, Mesh.COLOR_COMPONENT_COUNT,
				GL_FLOAT, false, stride, fb);
		glEnableVertexAttribArray(mColorLocation);

		glDrawArrays(GL_TRIANGLES, 0, mesh.nVertices());
	}

	private int mProgramObjectId;
	private int mPositionLocation;
	private int mColorLocation;
	private int mMatrixLocation;
}
