package com.js.geometryapp;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

import java.nio.FloatBuffer;

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
	}

	private static int sResultCode[] = new int[1];

	public void render(Mesh mesh) {
		// Not sok ure if this needs to be done every time
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
}
