package com.js.geometryapp;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

import android.graphics.Color;
import android.opengl.GLUtils;

public final class OurGLTools {

	/**
	 * Store current thread as the OpenGL rendering thread, for later calls to
	 * ensureRenderThread()
	 */
	public static void defineOpenGLThread() {
		if (DEBUG_ONLY_FEATURES)
			sOpenGLThread = Thread.currentThread();
	}

	/**
	 * Throw an exception if the current thread is not the OpenGL rendering
	 * thread (defined by call to defineOpenGLThread()).
	 */
	public static void ensureRenderThread() {
		if (DEBUG_ONLY_FEATURES) {
			if (Thread.currentThread() != sOpenGLThread)
				die("not in OpenGL rendering thread");
		}
	}

	/**
	 * Throw an exception if an OpenGL error has been generated
	 */
	public static void verifyNoError() {
		ensureRenderThread();
		int err = glGetError();
		if (err != 0) {
			die("OpenGL error! " + err + " : " + GLUtils.getEGLErrorString(err));
		}
	}

	private static void verifyNoProgramError(int programId, int objectParameter) {
		glGetProgramiv(programId, objectParameter, sResultCode, 0);
		if (!success()) {
			die("OpenGL error! Problem with program (parameter "
					+ objectParameter + "): " + glGetProgramInfoLog(programId));
		}
	}

	/**
	 * Create a program; die if unsuccessful
	 * 
	 * @return program id
	 */
	public static int createProgram() {
		ensureRenderThread();
		int programId = glCreateProgram();
		if (programId == 0) {
			die("OpenGL error! Unable to create program");
		}
		return programId;
	}

	/**
	 * Link a program; die if unsuccessful
	 */
	public static void linkProgram(int programId) {
		ensureRenderThread();
		glLinkProgram(programId);
		verifyNoProgramError(programId, GL_LINK_STATUS);
	}

	/**
	 * Validate a program; die if unsuccessful
	 */
	public static void validateProgram(int programId) {
		ensureRenderThread();
		glValidateProgram(programId);
		verifyNoProgramError(programId, GL_VALIDATE_STATUS);
	}

	public static void compileShader(int programId) {
		ensureRenderThread();
		glCompileShader(programId);
		glGetShaderiv(programId, GL_COMPILE_STATUS, sResultCode, 0);
		if (!success()) {
			die("OpenGL errror! Problem compiling shader: "
					+ glGetShaderInfoLog(programId));
		}
	}

	private static boolean success() {
		return sResultCode[0] != 0;
	}

	/**
	 * Specify program to use in subsequent calls to getProgramLocation()
	 */
	public static void setProgram(int programId) {
		ensureRenderThread();
		sProgramId = programId;
	}

	/**
	 * Get location of attribute or uniform within program; die if not found.
	 * Program id must have been previously set via setProgram()
	 * 
	 * @param attributeOrUniformName
	 *            name; must have prefix 'a_' or 'u_'
	 * @return
	 */
	public static int getProgramLocation(String attributeOrUniformName) {
		ensureRenderThread();
		int location = -1;
		if (attributeOrUniformName.startsWith("a_")) {
			location = glGetAttribLocation(sProgramId, attributeOrUniformName);
		} else if (attributeOrUniformName.startsWith("u_")) {
			location = glGetUniformLocation(sProgramId, attributeOrUniformName);
		} else
			die("unsupported prefix: " + attributeOrUniformName);
		if (location < 0)
			die("OpenGL error! No attribute/uniform found: "
					+ attributeOrUniformName);
		verifyNoError();
		return location;
	}

	public static void convertColorToOpenGL(int color, float[] rgba) {
		rgba[0] = Color.red(color) * (1.0f / 255.0f);
		rgba[1] = Color.green(color) * (1.0f / 255.0f);
		rgba[2] = Color.blue(color) * (1.0f / 255.0f);
		rgba[3] = Color.alpha(color) * (1.0f / 255.0f);
	}

	private static int sResultCode[] = new int[1];
	private static Thread sOpenGLThread;
	private static int sProgramId;
}
