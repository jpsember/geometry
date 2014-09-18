package com.js.geometryapp;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.graphics.Matrix;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.GeometryException;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
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
	private static final int EDGE_FLAG_INTERIOR = 1 << 0;

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
	 * Set arbitrary (simple, but possibly nonconvex) polygon as source;
	 * triangulate it and extract polygon strips
	 */
	public void setPolygon(Polygon p) {
		mPolygon = p;
		mConvexFlag = false;

		try {
			mContext = new GeometryContext(1965);
			triangulatePolygon();
			extractStrips();
		} catch (GeometryException e) {
			warning("caught: " + e);
			mException = e;
		}
	}

	/**
	 * Get any GeometryException that was produced during the triangulation /
	 * strip extraction process
	 */
	public GeometryException getError() {
		return mException;
	}

	private void triangulatePolygon() {
		PolygonTriangulator t = PolygonTriangulator.triangulator(mContext,
				mPolygon);
		t.triangulate();
	}

	private void extractStrips() {
		mVertexStrips.clear();
		mTrianglesExtracted = 0;

		findInteriorEdges();
		if (mInteriorEdgeCount % 3 != 0)
			GeometryException
					.raise("unexpected number of interior edges found: "
							+ mInteriorEdgeCount);

		mTrianglesExpected = mInteriorEdgeCount / 3;

		// Note: I originally did two passes, the first starting only at polygon
		// edges, to see if this reduced the strip count. It doesn't have much
		// of an effect.

		for (Edge edge : mContext.edgeBuffer()) {
			if (edge.visited())
				continue;
			if (!markedAsInteriorEdge(edge))
				continue;

			// Start a strip with the triangle to the left of this edge
			buildTriangleStrip(edge);
		}

		if (mTrianglesExtracted != mTrianglesExpected)
			GeometryException
					.raise("unexpected number of triangles generated for strips");
	}

	/**
	 * Build a triangle strip
	 * 
	 * @param baseEdge
	 *            edge with first triangle to its left
	 * @param parity
	 */
	private void buildTriangleStrip(Edge baseEdge) {
		sArray.clear();
		sArray.add(baseEdge.sourceVertex().point());
		sArray.add(baseEdge.destVertex().point());
		boolean parity = false;

		while (true) {
			baseEdge.setVisited(true);
			mTrianglesExtracted++;
			if (mTrianglesExtracted > mTrianglesExpected)
				GeometryException
						.raise("too many triangles generated for strips");

			Edge edge2 = nextEdgeInTriangle(baseEdge);
			Edge edge3 = prevEdgeInTriangle(baseEdge);

			sArray.add(edge2.destVertex().point());
			edge2.setVisited(true);
			edge3.setVisited(true);

			// See if we can extend this strip further
			Edge nextEdge = parity ? edge3 : edge2;
			nextEdge = nextEdge.dual();

			// If edge is not interior, or is already part of an earlier strip,
			// no.
			if (nextEdge.visited() || !nextEdge.hasFlags(EDGE_FLAG_INTERIOR))
				break;

			parity ^= true;
			baseEdge = nextEdge;
		}

		mVertexStrips.add(new TriangleStrip(sArray.asFloatBuffer(), sArray
				.size() / POSITION_COMPONENT_COUNT));
	}

	/**
	 * Render the polygon
	 * 
	 * @param additionalTransform
	 *            optional additional transformation to apply
	 */
	public void render(Matrix additionalTransform) {
		if (mException != null)
			return;

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
			for (TriangleStrip strip : mVertexStrips) {
				if (false) {
					warning("coloring strips to see what's being generated");
					float f[] = new float[4];
					OurGLTools.convertColorToOpenGL(OurGLTools.debugColor(), f);
					glUniform4fv(mColorLocation, 1, f, 0);
				}

				FloatBuffer fb = strip.floatBuffer();
				fb.position(0);
				int stride = POSITION_COMPONENT_COUNT
						* OurGLTools.BYTES_PER_FLOAT;

				glVertexAttribPointer(mPositionLocation,
						POSITION_COMPONENT_COUNT, GL_FLOAT, false, stride, fb);
				glEnableVertexAttribArray(mPositionLocation);
				glDrawArrays(GL_TRIANGLE_STRIP, 0, strip.numVertices());
			}
		}
	}

	private void prepareAttributes() {
		OurGLTools.setProgram(mProgram.getId());
		mPositionLocation = OurGLTools.getProgramLocation("a_Position");
		mColorLocation = OurGLTools.getProgramLocation("u_InputColor");
		mMatrixLocation = OurGLTools.getProgramLocation("u_Matrix");
	}

	/**
	 * Mark an edge as an interior edge, and push it onto the search stack; also
	 * increment the number of interior edges found
	 * 
	 * @param edge
	 */
	private void stackInteriorEdge(Edge edge) {
		mInteriorEdgeStack.add(edge);
		edge.addFlags(EDGE_FLAG_INTERIOR);
		mInteriorEdgeCount++;
	}

	/**
	 * Determine if this edge has been identified as an interior one
	 */
	private static boolean markedAsInteriorEdge(Edge edge) {
		return edge.hasFlags(EDGE_FLAG_INTERIOR);
	}

	private Edge popInteriorEdge() {
		return mInteriorEdgeStack.remove(mInteriorEdgeStack.size() - 1);
	}

	/**
	 * Mark all interior edges within mesh (i.e. edges whose area to their left
	 * lies inside the polygon). Also clears all the edge visited flags.
	 * 
	 * We perform a flood fill from polygon edges. It's slightly complicated by
	 * the fact that we don't have a data structure for faces.
	 * 
	 */
	private void findInteriorEdges() {

		mInteriorEdgeCount = 0;
		mInteriorEdgeStack.clear();

		mContext.clearMeshFlags(0, Edge.FLAG_VISITED | EDGE_FLAG_INTERIOR);

		for (Edge edge : mContext.edgeBuffer()) {
			if (markedAsInteriorEdge(edge))
				continue;

			if (!edge.isPolygon())
				continue;

			stackInteriorEdge(edge);

			while (!mInteriorEdgeStack.isEmpty()) {
				edge = popInteriorEdge();

				for (int pass = 0; pass < 2; pass++) {

					Edge edge2 = (pass == 0) ? nextEdgeInTriangle(edge)
							: prevEdgeInTriangle(edge);
					if (markedAsInteriorEdge(edge2))
						continue;

					stackInteriorEdge(edge2);

					// If this is an interior edge, add dual edge if not already
					// marked as interior edge
					if (edge2.isPolygon())
						continue;
					Edge dual = edge2.dual();
					if (markedAsInteriorEdge(dual))
						continue;
					stackInteriorEdge(dual);
				}
			}
		}
	}

	private static Edge nextEdgeInTriangle(Edge edge) {
		return edge.dual().prevEdge();
	}

	private static Edge prevEdgeInTriangle(Edge edge) {
		return edge.nextEdge().dual();
	}

	private static class TriangleStrip {
		public TriangleStrip(FloatBuffer buffer, int nVertices) {
			mFloatBuffer = buffer;
			mVertexCount = nVertices;
		}

		public FloatBuffer floatBuffer() {
			return mFloatBuffer;
		}

		public int numVertices() {
			return mVertexCount;
		}

		private FloatBuffer mFloatBuffer;
		private int mVertexCount;
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
	private ArrayList<TriangleStrip> mVertexStrips = new ArrayList();
	private int mTrianglesExtracted;
	private GeometryContext mContext;
	private ArrayList<Edge> mInteriorEdgeStack = new ArrayList();
	private int mInteriorEdgeCount;
	private int mTrianglesExpected;
	private GeometryException mException;
}
