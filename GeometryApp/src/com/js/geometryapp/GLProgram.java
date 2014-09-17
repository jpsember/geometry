package com.js.geometryapp;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

import java.nio.FloatBuffer;

import android.graphics.Matrix;

public class GLProgram {

	public void setTransformName(String name) {
		mTransformName = name;
	}

	public GLProgram(OurGLRenderer renderer, GLShader vertexShader,
			GLShader fragmentShader) {
		setTransformName(OurGLRenderer.TRANSFORM_NAME_DEVICE_TO_NDC);
		mRenderer = renderer;
		mProgramObjectId = glCreateProgram();
		if (mProgramObjectId == 0) {
			die("unable to create program");
		}
		glAttachShader(mProgramObjectId, vertexShader.getId());
		glAttachShader(mProgramObjectId, fragmentShader.getId());
		OurGLTools.linkProgram(mProgramObjectId);
		OurGLTools.validateProgram(mProgramObjectId);

		prepareAttributes();
	}

	public int getId() {
		return mProgramObjectId;
	}

	private void prepareAttributes() {
		OurGLTools.setProgram(mProgramObjectId);
		mPositionLocation = OurGLTools.getProgramLocation("a_Position");
		mColorLocation = OurGLTools.getProgramLocation("a_Color");
		mMatrixLocation = OurGLTools.getProgramLocation("u_Matrix");
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

	private void prepareMatrix(Matrix transform) {
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

				glUniformMatrix4fv(mMatrixLocation, 1, false, v4, 0);
			}
		}
	}

	public void render(Polyline p, Matrix transform) {
		glUseProgram(getId());
		prepareMatrix(transform);
		FloatBuffer fb = p.asFloatBuffer();
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

		// Until issue #18 is fixed, bump up line widths using this hack
		glLineWidth(p.lineWidth() * 1.5f);

		glDrawArrays(p.isClosed() ? GL_LINE_LOOP : GL_LINE_STRIP, 0,
				p.vertexCount());
	}

	/**
	 * Render mesh
	 * 
	 * @param mesh
	 * @param renderer
	 *            the renderer whose projection matrix is to be used
	 * @param transform
	 *            optional additional transformation to apply, or null
	 */
	public void render(Mesh mesh, Matrix transform) {
		glUseProgram(getId());
		// I'm assuming that each program remembers its own projection matrix
		prepareMatrix(transform);
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

		glDrawArrays(GL_TRIANGLES, 0, mesh.vertexCount());
	}

	private OurGLRenderer mRenderer;
	private int mPreparedProjectionMatrixId;
	private int mProgramObjectId;
	private int mPositionLocation;
	private int mColorLocation;
	private int mMatrixLocation;
	private String mTransformName;
}
