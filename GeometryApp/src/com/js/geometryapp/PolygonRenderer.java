package com.js.geometryapp;

import java.nio.FloatBuffer;

import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.R;

import static android.opengl.GLES20.*;
import static com.js.basic.Tools.*;

public class PolygonRenderer {

	public static void renderConvex(Polygon p) {
		render(p);
	}

	private static final int POSITION_COMPONENT_COUNT = 2; // x y

	/**
	 * Set color for polyline
	 */
	public static void setColor(int color) {
		OurGLTools.convertColorToOpenGL(color, sColor);
	}


	private static FloatBuffer convertPolygonToTriangleStrip(Polygon p) {
		sArray.clear();
		for (int i = 0; i < p.numVertices(); i++) {
			Point pt = p.vertex(i);
			sArray.add(pt);
		}
		return sArray.asFloatBuffer();
	}

	public static void prepareRenderer(OurGLRenderer renderer,
			String transformName) {
		GLShader vertexShader = GLShader.readVertexShader(renderer.context(),
				R.raw.polygon_vertex_shader);
		GLShader fragmentShader = GLShader.readFragmentShader(
				renderer.context(), R.raw.polygon_fragment_shader);
		sProgram = new GLProgram(renderer, vertexShader, fragmentShader);
		sProgram.setTransformName(AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		prepareAttributes();
	}

	private static void prepareAttributes() {
		OurGLTools.setProgram(sProgram.getId());
		sPositionLocation = OurGLTools.getProgramLocation("a_Position");
		sColorLocation = OurGLTools.getProgramLocation("u_InputColor");
		sMatrixLocation = OurGLTools.getProgramLocation("u_Matrix");
	}

	private static void render(Polygon polygon) {
		if (sProgram == null) die("renderer not prepared");
		
		glUseProgram(sProgram.getId());
		sProgram.prepareMatrix(null, sMatrixLocation);

		glUniform4fv(sColorLocation, 1, sColor, 0);

		FloatBuffer fb = convertPolygonToTriangleStrip(polygon);
		fb.position(0);
		int stride =  POSITION_COMPONENT_COUNT * OurGLTools.BYTES_PER_FLOAT;

		glVertexAttribPointer(sPositionLocation, POSITION_COMPONENT_COUNT,
				GL_FLOAT, false, stride, fb);
		glEnableVertexAttribArray(sPositionLocation);

		glDrawArrays(GL_TRIANGLE_FAN, 0, polygon.numVertices());
	}

	private static GLProgram sProgram;
	private static int sPositionLocation;
	private static int sColorLocation;
	private static int sMatrixLocation;
	private static float[] sColor = new float[4];

	private static FloatArray sArray = new FloatArray();
}
