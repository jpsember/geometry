package com.js.opengl;

import android.content.Context;
import static android.opengl.GLES20.*;
import static com.js.android.Tools.*;

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
		GLTools.compileShader(getId());
	}

	private void dispose() {
		if (getId() != 0) {
			glDeleteShader(mShaderObjectId);
			mShaderObjectId = 0;
		}
	}

	// TODO: Issue #6; refactor to avoid use of finalizer
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

	private int mType;
	private String mSource;
	private int mShaderObjectId;
}
