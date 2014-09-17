package com.js.geometryapp;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

import android.opengl.GLUtils;

public class OurGLTools {

	/**
	 * Store current thread as the OpenGL rendering thread, for later calls to
	 * ensureRenderThread()
	 */
	public static void defineOpenGLThread() {
		sOpenGLThread = Thread.currentThread();
	}

	/**
	 * Throw an exception if the current thread is not the OpenGL rendering
	 * thread (defined by call to defineOpenGLThread())
	 */
	public static void ensureRenderThread() {
		if (Thread.currentThread() != sOpenGLThread)
			die("not in OpenGL rendering thread");
	}

	/**
	 * Throw an exception if an OpenGL error has been generated
	 */
	public static void verifyNoError() {
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
	 * Link a program; die if unsuccessful
	 */
	public static void linkProgram(int programId) {
		glLinkProgram(programId);
		verifyNoProgramError(programId, GL_LINK_STATUS);
	}

	/**
	 * Validate a program; die if unsuccessful
	 */
	public static void validateProgram(int programId) {
		glValidateProgram(programId);
		verifyNoProgramError(programId, GL_VALIDATE_STATUS);
	}

	private static boolean success() {
		return sResultCode[0] != 0;
	}

	private static int sResultCode[] = new int[1];
	private static Thread sOpenGLThread;
}
