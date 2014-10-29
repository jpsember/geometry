package com.js.geometryapp;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static com.js.basic.Tools.*;

import java.nio.FloatBuffer;
import java.util.Collection;

import android.graphics.Matrix;

import com.js.android.MyActivity;
import com.js.geometry.FloatArray;
import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.opengl.GLProgram;
import com.js.opengl.GLShader;
import com.js.opengl.GLTools;
import com.js.opengl.OurGLRenderer;

public class PolylineProgram {

	private static final int POSITION_COMPONENT_COUNT = 2; // x y

	/**
	 * Constructor
	 * 
	 * @param renderer
	 * @param transformName
	 *            which transform is to be used for this program
	 */
	public PolylineProgram(OurGLRenderer renderer, String transformName) {
		GLShader vertexShader = GLShader.readVertexShader(renderer.context(),
				R.raw.polyline_vertex_shader);
		GLShader fragmentShader = GLShader.readFragmentShader(
				renderer.context(), R.raw.polyline_fragment_shader);
		sProgram = new GLProgram(renderer, vertexShader, fragmentShader);
		sProgram.setTransformName(transformName);
		prepareAttributes();
	}

	private void prepareAttributes() {
		GLTools.setProgram(sProgram.getId());
		sPositionLocation = GLTools.getProgramLocation("a_Position");
		sColorLocation = GLTools.getProgramLocation("u_InputColor");
		sMatrixLocation = GLTools.getProgramLocation("u_Matrix");
	}

	/**
	 * Set color of subsequent render operations with this program
	 */
	public void setColor(int color) {
		GLTools.convertColorToOpenGL(color, mColor);
		mColorValid = false;
	}

	private FloatBuffer compileVertices(Collection<Point> vertices) {
		FloatArray mFloatArray = new FloatArray();
		for (Point pt : vertices) {
			mFloatArray.add(pt);
		}
		return mFloatArray.asFloatBuffer();
	}

	/**
	 * Render a polyline
	 * 
	 * @param vertices
	 * @param additionalTransform
	 *            optional additional transformation to apply
	 */
	public void render(Collection<Point> vertices, Matrix additionalTransform,
			boolean closed) {
		glUseProgram(sProgram.getId());

		// We only need to send color when it changes
		if (!mColorValid) {
			glUniform4fv(sColorLocation, 1, mColor, 0);
			mColorValid = true;
		}
		sProgram.prepareMatrix(additionalTransform, sMatrixLocation);

		FloatBuffer fb = compileVertices(vertices);
		fb.position(0);
		int stride = (POSITION_COMPONENT_COUNT) * BYTES_PER_FLOAT;

		glVertexAttribPointer(sPositionLocation, POSITION_COMPONENT_COUNT,
				GL_FLOAT, false, stride, fb);
		glEnableVertexAttribArray(sPositionLocation);

		float lineScaleFactor = MyActivity.getResolutionInfo()
				.inchesToPixelsAlgorithm(.01f);
		glLineWidth(lineScaleFactor * RenderTools.getRenderLineWidth());
		glDrawArrays(closed ? GL_LINE_LOOP : GL_LINE_STRIP, 0, vertices.size());
	}

	private GLProgram sProgram;
	private int sPositionLocation;
	private int sColorLocation;
	private int sMatrixLocation;
	private float[] mColor = new float[4];
	private boolean mColorValid;

}
