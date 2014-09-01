package com.js.geometryapp;

import android.content.Context;
import static android.opengl.GLES20.*;
import static com.js.android.Tools.readTextFileResource;

import static com.js.basic.Tools.*;

public class GLShader {

	private GLShader(int type) {
		mType = type;
	}

	private void readSource(Context context, int resourceId) {
		mSource = readTextFileResource(context, resourceId);
	}

	private void compileSource() {
		mShaderObjectId = glCreateShader(mType);
		if (getId() == 0)
			die("unable to create shader object: " + this);
		glShaderSource(getId(), mSource);
		glCompileShader(getId());

		glGetShaderiv(getId(), GL_COMPILE_STATUS, sResultCode, 0);
		if (!success()) {
			String logInfo = glGetShaderInfoLog(getId());
			warning("failed to compile shader source:\n" + logInfo);
		}
	}

	private void dispose() {
		if (getId() != 0) {
			glDeleteShader(mShaderObjectId);
			mShaderObjectId = 0;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(nameOf(this));
		sb.append(" type: ");
		sb.append(mType == GL_VERTEX_SHADER ? "VERTEX" : "FRAGMENT");
		sb.append(" id:");
		sb.append(getId());
		return sb.toString();
	}

	public static GLShader readVertexShader(Context context, int resourceId) {
		GLShader s = new GLShader(GL_VERTEX_SHADER);
		s.readSource(context, resourceId);
		s.compileSource();
		return s;
	}

	public static GLShader readFragmentShader(Context context, int resourceId) {
		GLShader s = new GLShader(GL_FRAGMENT_SHADER);
		s.readSource(context, resourceId);
		s.compileSource();
		return s;
	}

	public final int getId() {
		return mShaderObjectId;
	}

	private static boolean success() {
		return sResultCode[0] != 0;
	}

	private static int sResultCode[] = new int[1];

	private int mType;
	private String mSource;
	private int mShaderObjectId;
}
