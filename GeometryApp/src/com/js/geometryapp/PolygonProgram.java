package com.js.geometryapp;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.graphics.Matrix;

import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.R;

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

	private static final int POSITION_COMPONENT_COUNT = 2; // x y

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
		OurGLTools.convertColorToOpenGL(color, mColor);
		mColorValid = false;
	}

	/**
	 * Set convex polygon as source
	 */
	public void setConvexPolygon(Polygon p) {
		mPolygon = p;
		mConvexFlag = true;

		sArray.clear();
		for (int i = 0; i < p.numVertices(); i++) {
			Point pt = p.vertex(i);
			sArray.add(pt);
		}
		mVertexFan = sArray.asFloatBuffer();
	}

	/**
	 * Set arbitrary (simple, but possibly nonconvex) polygon as source
	 */
	public void setPolygon(Polygon p) {
		mPolygon = p;
		mConvexFlag = false;
		unimp("triangulate polygon into strips");
	}

	/**
	 * Render the polygon
	 * 
	 * @param additionalTransform
	 *            optional additional transformation to apply
	 */
	public void render(Matrix additionalTransform) {

		glUseProgram(mProgram.getId());
		mProgram.prepareMatrix(additionalTransform, mMatrixLocation);

		// We only need to send color when it changes
		if (!mColorValid) {
			glUniform4fv(mColorLocation, 1, mColor, 0);
			mColorValid = true;
		}

		if (mConvexFlag) {
			FloatBuffer fb = mVertexFan;
			fb.position(0);
			int stride = POSITION_COMPONENT_COUNT * OurGLTools.BYTES_PER_FLOAT;

			glVertexAttribPointer(mPositionLocation, POSITION_COMPONENT_COUNT,
					GL_FLOAT, false, stride, fb);
			glEnableVertexAttribArray(mPositionLocation);
			glDrawArrays(GL_TRIANGLE_FAN, 0, mPolygon.numVertices());
		} else {
			unimp("render strips " + mVertexStrips.size());
		}
	}

	private void prepareAttributes() {
		OurGLTools.setProgram(mProgram.getId());
		mPositionLocation = OurGLTools.getProgramLocation("a_Position");
		mColorLocation = OurGLTools.getProgramLocation("u_InputColor");
		mMatrixLocation = OurGLTools.getProgramLocation("u_Matrix");
	}

	private static FloatArray sArray = new FloatArray();

	private boolean mConvexFlag;
	private Polygon mPolygon;
	private GLProgram mProgram;
	private int mPositionLocation;
	private int mColorLocation;
	private int mMatrixLocation;
	private float[] mColor = new float[4];
	private boolean mColorValid;
	private FloatBuffer mVertexFan;
	private ArrayList<FloatBuffer> mVertexStrips = new ArrayList();
}
