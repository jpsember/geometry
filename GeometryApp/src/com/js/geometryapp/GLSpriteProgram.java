package com.js.geometryapp;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

import java.nio.FloatBuffer;

//import javax.microedition.khronos.opengles.GL10;

import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.geometry.Rect;

import android.content.Context;
import android.graphics.Matrix;

public class GLSpriteProgram {

	private static final int POSITION_COMPONENT_COUNT = 2; // x y
	private static final int TEXTURE_COMPONENT_COUNT = 2; // u v
	private static final int TOTAL_COMPONENTS = POSITION_COMPONENT_COUNT
			+ TEXTURE_COMPONENT_COUNT;

	public static void prepare(Context context) {
		prepareShaders(context);
		prepareProgram();
		sPrepared = true;
	}

	public static void setProjection(Matrix m) {
		ensurePrepared();
		glUseProgram(sProgramObjectId);

		// Transform 2D screen->NDC matrix to a 3D version
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

		glUniformMatrix4fv(sMatrixLocation, 1, false, v4, 0);
		sMatrixPrepared = true;
	}

	private static void prepareShaders(Context context) {
		sVertexShader = GLShader.readVertexShader(context,
				R.raw.vertex_shader_texture);
		sFragmentShader = GLShader.readFragmentShader(context,
				R.raw.fragment_shader_texture);
	}

	private static void prepareProgram() {
		sProgramObjectId = glCreateProgram();
		if (sProgramObjectId == 0) {
			die("unable to create program");
		}
		glAttachShader(sProgramObjectId, sVertexShader.getId());
		glAttachShader(sProgramObjectId, sFragmentShader.getId());
		glLinkProgram(sProgramObjectId);

		glGetProgramiv(sProgramObjectId, GL_LINK_STATUS, sResultCode, 0);
		if (!success()) {
			warning("failed to link program:\n"
					+ glGetProgramInfoLog(sProgramObjectId));
			return;
		}
		validate();
		prepareAttributes();
	}

	private static void validate() {
		glValidateProgram(sProgramObjectId);

		glGetProgramiv(sProgramObjectId, GL_VALIDATE_STATUS, sResultCode, 0);
		if (!success()) {
			warning("failed to validate program:\n"
					+ glGetProgramInfoLog(sProgramObjectId));
			return;
		}
	}

	private static boolean success() {
		return sResultCode[0] != 0;
	}

	private static void prepareAttributes() {
		// Must agree with vertex_shader_texture.glsl
		sPositionLocation = glGetAttribLocation(sProgramObjectId, "a_Position");
		sSpritePositionLocation = glGetUniformLocation(sProgramObjectId,
				"u_SpritePosition");
		sTextureCoordinateLocation = glGetAttribLocation(sProgramObjectId,
				"a_TexCoordinate");
		sMatrixLocation = glGetUniformLocation(sProgramObjectId, "u_Matrix");
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


	private static void ensurePrepared() {
		if (!sPrepared)
			throw new IllegalStateException("GLSpriteProgram not prepared");

	}

	public GLSpriteProgram(GLTexture texture, Rect textureWindow) {
		ensurePrepared();
		mTexture = texture;
		mTextureWindow = textureWindow;

		constructVertexInfo();
	}

	private void constructVertexInfo() {
		FloatArray mMesh = new FloatArray();

		Point p0 = new Point(0, 0);
		Point p2 = new Point(mTextureWindow.width, mTextureWindow.height);
		Point p1 = new Point(p2.x, p0.y);
		Point p3 = new Point(p0.x, p2.y);

		Point t0 = new Point(mTextureWindow.x / mTexture.width(),
				mTextureWindow.endY() / mTexture.height());
		Point t2 = new Point(mTextureWindow.endX() / mTexture.width(),
				mTextureWindow.y / mTexture.height());
		Point t1 = new Point(t2.x, t0.y);
		Point t3 = new Point(t0.x, t2.y);

		mMesh.add(p0);
		mMesh.add(t0);
		mMesh.add(p1);
		mMesh.add(t1);
		mMesh.add(p2);
		mMesh.add(t2);

		mMesh.add(p0);
		mMesh.add(t0);
		mMesh.add(p2);
		mMesh.add(t2);
		mMesh.add(p3);
		mMesh.add(t3);

		mVertexData = mMesh.asFloatBuffer();
	}

	private Point mPosition = new Point();

	public void setPosition(float x, float y) {
		mPosition.x = x;
		mPosition.y = y;
	}

	/**
	 */
	public void render() {
		ASSERT(sMatrixPrepared);
		glUseProgram(sProgramObjectId);

		// Specify offset
		{
			glUniform2f(sSpritePositionLocation, mPosition.x, mPosition.y);
		}

		mTexture.select();

		FloatBuffer fb = mVertexData;
		fb.position(0);
		int stride = TOTAL_COMPONENTS * Mesh.BYTES_PER_FLOAT;

		glVertexAttribPointer(sPositionLocation,
 POSITION_COMPONENT_COUNT,
				GL_FLOAT, false, stride,
				fb);
		glEnableVertexAttribArray(sPositionLocation);

		fb.position(POSITION_COMPONENT_COUNT);
		glVertexAttribPointer(sTextureCoordinateLocation,
				TEXTURE_COMPONENT_COUNT, GL_FLOAT, false, stride,
				fb);
		glEnableVertexAttribArray(sTextureCoordinateLocation);
		glDrawArrays(GL_TRIANGLES, 0, 6);
	}

	private FloatBuffer mVertexData;

	private static boolean sPrepared;
	private GLTexture mTexture;
	private Rect mTextureWindow;
	private static int sProgramObjectId;
	private static int sPositionLocation;
	private static int sTextureCoordinateLocation;
	private static int sMatrixLocation;
	private static int sSpritePositionLocation;
	private static GLShader sVertexShader;
	private static GLShader sFragmentShader;
	private static boolean sMatrixPrepared;
}
