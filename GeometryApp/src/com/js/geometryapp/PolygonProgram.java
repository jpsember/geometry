package com.js.geometryapp;

import java.nio.FloatBuffer;

import android.graphics.Matrix;

import com.js.basic.Point;
import com.js.geometry.PolygonMesh;
import com.js.geometry.R;
import com.js.opengl.GLProgram;
import com.js.opengl.GLShader;
import com.js.opengl.GLTools;
import com.js.opengl.OurGLRenderer;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

/**
 * A program to render polygons
 * 
 * Construct a program with a particular renderer and transform. You can change
 * the state by setting the color, or setting which polygon is to be rendered.
 * Render the polygon by calling render()
 * 
 */
public class PolygonProgram {

	/**
	 * Constructor
	 * 
	 * @param renderer
	 * @param transformName
	 *            which transform is to be used for this program
	 */
	public PolygonProgram(OurGLRenderer renderer, String transformName) {
		GLShader vertexShader = GLShader.readVertexShader(renderer.context(),
				R.raw.polygon_vertex_shader);
		GLShader fragmentShader = GLShader.readFragmentShader(
				renderer.context(), R.raw.polygon_fragment_shader);
		mProgram = new GLProgram(renderer, vertexShader, fragmentShader);
		mProgram.setTransformName(transformName);
		prepareAttributes();
	}

	/**
	 * Set color of subsequent render operations with this program
	 */
	public void setColor(int color) {
		GLTools.convertColorToOpenGL(color, mColor);
		mColorValid = false;
	}

	/**
	 * Render the polygon
	 * 
	 * @param mesh
	 * @param offset
	 *            optional offset to apply to mesh
	 */
	public void render(PolygonMesh mesh, Point translation) {
		render(mesh, translation, null);
	}

	public void render(PolygonMesh mesh) {
		render(mesh, null, null);
	}

	private static final int[] sMeshTypeToGL = { GL_TRIANGLES, // TYPE_TRIANGLES
			GL_TRIANGLE_FAN, // TYPE_FANS
			GL_TRIANGLE_STRIP, // TYPE_STRIPS
	};

	/**
	 * Render the polygon
	 * 
	 * @param translation
	 *            optional translation to apply (before further transformations)
	 * @param additionalTransform
	 *            optional additional transformation to apply
	 */
	public void render(PolygonMesh mesh, Point translation,
			Matrix additionalTransform) {
		if (mesh.getError() != null) {
			warning("mesh error " + mesh.getError());
			return;
		}

		glUseProgram(mProgram.getId());
		mProgram.prepareMatrix(additionalTransform, mMatrixLocation);
		if (translation == null)
			translation = Point.ZERO;
		glUniform2f(mTranslationLocation, translation.x, translation.y);

		// We only need to send color when it changes
		if (!mColorValid) {
			glUniform4fv(mColorLocation, 1, mColor, 0);
			mColorValid = true;
		}

		FloatBuffer fb = mesh.getVertexBuffer();
		fb.position(0);
		int stride = PolygonMesh.VERTEX_COMPONENTS * BYTES_PER_FLOAT;

		glVertexAttribPointer(mPositionLocation, PolygonMesh.VERTEX_COMPONENTS,
				GL_FLOAT, false, stride, fb);
		glEnableVertexAttribArray(mPositionLocation);

		glDrawArrays(sMeshTypeToGL[mesh.type()], 0, mesh.getVertexCount());
	}

	private void prepareAttributes() {
		GLTools.setProgram(mProgram.getId());
		mPositionLocation = GLTools.getProgramLocation("a_Position");
		mColorLocation = GLTools.getProgramLocation("u_InputColor");
		mMatrixLocation = GLTools.getProgramLocation("u_Matrix");
		mTranslationLocation = GLTools.getProgramLocation("u_Translation");
	}

	private GLProgram mProgram;
	private int mPositionLocation;
	private int mColorLocation;
	private int mMatrixLocation;
	private int mTranslationLocation;
	private float[] mColor = new float[4];
	private boolean mColorValid;
}
