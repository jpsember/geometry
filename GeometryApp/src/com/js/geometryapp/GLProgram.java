package com.js.geometryapp;

import static android.opengl.GLES20.*;

import android.graphics.Matrix;

public class GLProgram {

	public void setTransformName(String name) {
		mTransformName = name;
	}

	public GLProgram(OurGLRenderer renderer, GLShader vertexShader,
			GLShader fragmentShader) {
		setTransformName(OurGLRenderer.TRANSFORM_NAME_DEVICE_TO_NDC);
		mRenderer = renderer;
		mProgramObjectId = OurGLTools.createProgram();
		glAttachShader(mProgramObjectId, vertexShader.getId());
		glAttachShader(mProgramObjectId, fragmentShader.getId());
		OurGLTools.linkProgram(mProgramObjectId);
		OurGLTools.validateProgram(mProgramObjectId);
	}

	public int getId() {
		return mProgramObjectId;
	}

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
	 * Prepare transform matrix for program, if necessary
	 * 
	 * @param transform
	 *            optional additional transformation matrix to concatenate to
	 *            the render's prepared matrix
	 * @param matrixLocation
	 *            location of matrix within program
	 */
	protected void prepareMatrix(Matrix transform, int matrixLocation) {

		// Only do this if the previously prepared matrix is no longer valid, or
		// if we need to include an object-specific transformation

		if (mRenderer.projectionMatrixId() != mPreparedProjectionMatrixId
				|| transform != null) {
			Matrix mainTransform = mRenderer.getTransform(mTransformName);
			Matrix m;
			if (transform != null) {
				// We won't want to reuse this particular prepared matrix again
				mPreparedProjectionMatrixId = 0;
				// Concatenate the mesh matrix to the projection matrix
				m = new Matrix(transform);
				m.postConcat(mainTransform);
			} else {
				mPreparedProjectionMatrixId = mRenderer.projectionMatrixId();
				m = mainTransform;
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

				glUniformMatrix4fv(matrixLocation, 1, false, v4, 0);
			}
		}
	}

	private OurGLRenderer mRenderer;
	private int mPreparedProjectionMatrixId;
	private int mProgramObjectId;
	private String mTransformName;
}
