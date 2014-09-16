package com.js.geometryapp;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glUniform2f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glValidateProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static com.js.basic.Tools.*;

import java.nio.FloatBuffer;

import android.content.Context;

import com.js.geometry.Point;
import com.js.geometry.R;

public class SpriteContext {
	private static final int POSITION_COMPONENT_COUNT = 2; // x y
	private static final int TEXTURE_COMPONENT_COUNT = 2; // u v
	private static final int TOTAL_COMPONENTS = POSITION_COMPONENT_COUNT
			+ TEXTURE_COMPONENT_COUNT;

	/**
	 * Must be called by onSurfaceCreated()
	 * 
	 * @param context
	 */
	public static void prepare(OurGLRenderer renderer) {
		sRenderer = renderer;

		sNormalContext = new SpriteContext();
	}

	private void prepareShaders() {
		Context context = sRenderer.context();
		mVertexShader = GLShader.readVertexShader(context,
				R.raw.vertex_shader_texture);
		mFragmentShader = GLShader.readFragmentShader(context,
				R.raw.fragment_shader_texture);
	}

	/**
	 * Select this context's OpenGL program. Also, prepares the program (and
	 * associated shaders) if it hasn't already been done. We don't do this in
	 * the constructor, since it must be done within the OpenGL thread
	 */
	private void activateProgram() {
		int currentSurfaceId = sRenderer.surfaceId();
		if (currentSurfaceId != mPreparedSurfaceId) {
			mPreparedSurfaceId = currentSurfaceId;
			prepareShaders();
			prepareProgram();
		}

		glUseProgram(mProgramObjectId);
	}

	private int mPreparedSurfaceId;

	private void prepareProgram() {
		mProgramObjectId = glCreateProgram();
		if (mProgramObjectId == 0) {
			die("unable to create program");
		}
		glAttachShader(mProgramObjectId, mVertexShader.getId());
		glAttachShader(mProgramObjectId, mFragmentShader.getId());
		glLinkProgram(mProgramObjectId);

		glGetProgramiv(mProgramObjectId, GL_LINK_STATUS, sResultCode, 0);
		if (!success()) {
			warning("failed to link program:\n"
					+ glGetProgramInfoLog(mProgramObjectId));
			return;
		}
		validate();
		prepareAttributes();
	}

	private void validate() {
		glValidateProgram(mProgramObjectId);

		glGetProgramiv(mProgramObjectId, GL_VALIDATE_STATUS, sResultCode, 0);
		if (!success()) {
			warning("failed to validate program:\n"
					+ glGetProgramInfoLog(mProgramObjectId));
			return;
		}
	}

	private boolean success() {
		return sResultCode[0] != 0;
	}

	private void prepareAttributes() {
		// Must agree with vertex_shader_texture.glsl
		mPositionLocation = glGetAttribLocation(mProgramObjectId, "a_Position");
		mSpritePositionLocation = glGetUniformLocation(mProgramObjectId,
				"u_SpritePosition");
		mTextureCoordinateLocation = glGetAttribLocation(mProgramObjectId,
				"a_TexCoordinate");
		mMatrixLocation = glGetUniformLocation(mProgramObjectId, "u_Matrix");
	}

	public void renderSprite(GLTexture mTexture, FloatBuffer vertexData,
			Point mPosition) {
		activateProgram();

		prepareProjection();

		// Specify offset
		glUniform2f(mSpritePositionLocation, mPosition.x, mPosition.y);

		mTexture.select();

		vertexData.position(0);
		int stride = TOTAL_COMPONENTS * Mesh.BYTES_PER_FLOAT;

		glVertexAttribPointer(mPositionLocation, POSITION_COMPONENT_COUNT,
				GL_FLOAT, false, stride, vertexData);
		glEnableVertexAttribArray(mPositionLocation);

		vertexData.position(POSITION_COMPONENT_COUNT);
		glVertexAttribPointer(mTextureCoordinateLocation,
				TEXTURE_COMPONENT_COUNT, GL_FLOAT, false, stride, vertexData);
		glEnableVertexAttribArray(mTextureCoordinateLocation);
		glDrawArrays(GL_TRIANGLES, 0, 6);
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

	private void prepareProjection() {
		int currentProjectionMatrixId = sRenderer.projectionMatrixId();
		if (currentProjectionMatrixId == mPreparedProjectionMatrixId)
			return;
		mPreparedProjectionMatrixId = currentProjectionMatrixId;

		// Transform 2D screen->NDC matrix to a 3D version
		float v3[] = new float[9];
		sRenderer.projectionMatrix().getValues(v3);

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

	public static SpriteContext normalContext() {
		return sNormalContext;
	}

	private static int sResultCode[] = new int[1];
	private static OurGLRenderer sRenderer;
	private static SpriteContext sNormalContext;

	private int mPreparedProjectionMatrixId;
	private int mProgramObjectId;
	private int mPositionLocation;
	private int mTextureCoordinateLocation;
	private int mMatrixLocation;
	private int mSpritePositionLocation;
	private GLShader mVertexShader;
	private GLShader mFragmentShader;

}
