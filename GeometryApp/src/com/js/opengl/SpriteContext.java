package com.js.opengl;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

import java.nio.FloatBuffer;

import android.content.Context;

import com.js.geometry.Point;
import com.js.geometry.R;

public class SpriteContext {
	protected static final int POSITION_COMPONENT_COUNT = 2; // x y
	protected static final int TEXTURE_COMPONENT_COUNT = 2; // u v
	protected static final int TOTAL_COMPONENTS = POSITION_COMPONENT_COUNT
			+ TEXTURE_COMPONENT_COUNT;

	public SpriteContext(String transformName, boolean tintMode) {
		mTransformName = transformName;
		if (tintMode)
			setTintMode();
	}

	/**
	 * Make this sprite context generate tinted sprites
	 */
	private void setTintMode() {
		mTintMode = true;
		mTintColor = new float[4];
	}

	/**
	 * Set tint color; context must have tint mode set
	 */
	public void setTintColor(int color) {
		ASSERT(mTintMode);
		GLTools.convertColorToOpenGL(color, mTintColor);
	}

	public void renderSprite(GLTexture mTexture, FloatBuffer vertexData,
			Point mPosition) {
		activateProgram();

		prepareProjection();

		glUniform2f(mSpritePositionLocation, mPosition.x, mPosition.y);

		if (mTintMode) {
			// Send one vec4 (the second parameter; this was a gotcha)
			glUniform4fv(mColorLocation, 1, mTintColor, 0);
		}

		mTexture.select();

		GLTools.verifyNoError();
		vertexData.position(0);
		int stride = TOTAL_COMPONENTS * BYTES_PER_FLOAT;

		glVertexAttribPointer(mPositionLocation, POSITION_COMPONENT_COUNT,
				GL_FLOAT, false, stride, vertexData);
		glEnableVertexAttribArray(mPositionLocation);

		vertexData.position(POSITION_COMPONENT_COUNT);
		glVertexAttribPointer(mTextureCoordinateLocation,
				TEXTURE_COMPONENT_COUNT, GL_FLOAT, false, stride, vertexData);
		glEnableVertexAttribArray(mTextureCoordinateLocation);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		GLTools.verifyNoError();
	}

	/**
	 * Must be called by onSurfaceCreated()
	 * 
	 * @param context
	 */
	public static void prepare(OurGLRenderer renderer) {
		sRenderer = renderer;

		sNormalContext = new SpriteContext(
				OurGLRenderer.TRANSFORM_NAME_DEVICE_TO_NDC, false);
	}

	private void prepareShaders() {
		Context context = sRenderer.context();
		mVertexShader = GLShader.readVertexShader(context,
				R.raw.vertex_shader_texture);
		mFragmentShader = GLShader.readFragmentShader(context,
				mTintMode ? R.raw.fragment_shader_mask
						: R.raw.fragment_shader_texture);
	}

	/**
	 * Select this context's OpenGL program. Also, prepares the program (and
	 * associated shaders) if it hasn't already been done. We don't do this in
	 * the constructor, since it must be done within the OpenGL thread
	 */
	protected void activateProgram() {
		int currentSurfaceId = sRenderer.surfaceId();
		if (currentSurfaceId != mPreparedSurfaceId) {
			mPreparedSurfaceId = currentSurfaceId;
			prepareShaders();
			prepareProgram();
		}

		glUseProgram(mProgramObjectId);
	}

	private void prepareProgram() {
		mProgramObjectId = GLTools.createProgram();
		glAttachShader(mProgramObjectId, mVertexShader.getId());
		glAttachShader(mProgramObjectId, mFragmentShader.getId());
		GLTools.linkProgram(mProgramObjectId);
		GLTools.validateProgram(mProgramObjectId);
		prepareAttributes();
	}

	private void prepareAttributes() {
		GLTools.setProgram(mProgramObjectId);
		mPositionLocation = GLTools.getProgramLocation("a_Position");
		mSpritePositionLocation = GLTools
				.getProgramLocation("u_SpritePosition");
		mTextureCoordinateLocation = GLTools
				.getProgramLocation("a_TexCoordinate");
		mMatrixLocation = GLTools.getProgramLocation("u_Matrix");

		if (mTintMode) {
			mColorLocation = GLTools.getProgramLocation("u_InputColor");
		}
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
		sRenderer.getTransform(mTransformName).getValues(v3);

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

	private static OurGLRenderer sRenderer;
	private static SpriteContext sNormalContext;

	private int mPreparedProjectionMatrixId;
	private int mPreparedSurfaceId;
	private int mProgramObjectId;
	private int mPositionLocation;
	private int mTextureCoordinateLocation;
	private int mMatrixLocation;
	private int mSpritePositionLocation;
	private boolean mTintMode;
	private GLShader mVertexShader;
	private GLShader mFragmentShader;
	private String mTransformName;
	private int mColorLocation;
	private float[] mTintColor;
}
