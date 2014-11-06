package com.js.geometry;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;

import com.js.geometryapp.RenderTools;

import static com.js.basic.Tools.*;
import static com.js.geometry.MyMath.*;

public final class Mesh implements Renderable {

	/**
	 * Construct a list of all the edges
	 * 
	 * @return array of edges
	 */
	public ArrayList<Edge> constructListOfEdges() {
		return constructListOfEdges(false);
	}

	/**
	 * Construct a list of all the edges
	 * 
	 * @param omitDuals
	 *            if true, exactly one of an edge or its dual will appear in the
	 *            array
	 * @return array of edges
	 */
	public ArrayList<Edge> constructListOfEdges(boolean omitDuals) {
		ArrayList<Edge> edges = new ArrayList();
		for (Vertex vertex : mVertexBuffer) {
			Edge edge = vertex.edges();
			if (edge == null)
				continue;
			while (true) {
				if (!omitDuals || edge.angle() >= 0)
					edges.add(edge);
				edge = edge.nextEdge();
				if (edge == vertex.edges())
					break;
			}
		}
		return edges;
	}

	public void clear() {
		mVertexBuffer.clear();
	}

	public void clearFlags(int vertexFlags, int edgeFlags) {
		if (vertexFlags != 0) {
			for (Vertex v : mVertexBuffer) {
				v.clearFlags(vertexFlags);
			}
		}
		if (edgeFlags != 0) {
			for (Edge edge : constructListOfEdges(true)) {
				edge.clearFlags(edgeFlags);
				edge.dual().clearFlags(edgeFlags);
			}
		}
	}

	public Edge addEdge(Vertex v0, Vertex v1) {
		return addEdge(v0, v1, false);
	}

	public Edge addEdge(Vertex v0, Vertex v1, boolean ifNoEdgeExists) {
		if (ifNoEdgeExists) {
			Edge existing = edgeExistsBetween(v0, v1);
			if (existing != null)
				return existing;
		}
		Edge edge = new Edge();
		Edge dual = new Edge();
		edge.setDual(dual);
		dual.setDual(edge);

		Point delta = new Point(v1.x - v0.x, v1.y - v0.y);
		float angle = pseudoPolarAngle(delta);

		edge.setAngle(angle);
		edge.setDestVertex(v1);

		dual.setAngle(normalizePseudoAngle(angle + PSEUDO_ANGLE_RANGE_12));
		dual.setDestVertex(v0);

		addEdgeToVertex(edge, v0);
		addEdgeToVertex(dual, v1);

		return edge;
	}

	public Edge edgeExistsBetween(Vertex sourceVert, Vertex destVert) {
		Edge foundEdge = null;
		Edge edge = sourceVert.edges();
		if (edge != null) {

			while (true) {
				if (edge.destVertex() == destVert) {
					foundEdge = edge;
					break;
				}
				edge = edge.nextEdge();
				if (edge == sourceVert.edges())
					break;
			}
		}
		return foundEdge;
	}

	public Edge polygonEdgeFromVertex(Vertex v) {
		Edge output = null;
		do {
			Edge edge = v.edges();
			if (edge == null)
				break;
			if (edge.isPolygon()) {
				output = edge;
				break;
			}
			edge = edge.nextEdge();
			if (edge.isPolygon()) {
				output = edge;
			}
		} while (false);
		if (output == null) {
			GeometryException
					.raise("cannot find polygonal edge leaving vertex " + v);
		}
		return output;
	}

	public Edge polygonEdgeToVertex(Vertex v) {
		Edge output = null;
		do {
			Edge edge = v.edges();
			if (edge == null)
				break;
			if (edge.isPolygon()) {
				edge = edge.nextEdge();
			}
			edge = edge.dual();
			if (edge.isPolygon()) {
				output = edge;
			}
		} while (false);
		if (output == null) {
			GeometryException
					.raise("cannot find polygonal edge entering vertex " + v);
		}
		return output;
	}

	public int numVertices() {
		return mVertexBuffer.size();
	}

	public Vertex vertex(int index) {
		return mVertexBuffer.get(index);
	}

	public Vertex addVertex(Point location) {
		testForOverflow(location.x);
		testForOverflow(location.y);
		Vertex v = new Vertex(mVertexBuffer.size(), location);
		mVertexBuffer.add(v);
		return v;
	}

	/**
	 * Delete a vertex. Deletes all edges incident with vertex as well. The
	 * order of the vertices within the vertex buffer may change as a result
	 */
	public void deleteVertex(Vertex vertex) {

		// Remove all edges incident with the vertex
		Edge edge = vertex.edges();
		if (edge != null) {
			while (true) {
				Edge nextEdge = edge.nextEdge();
				deleteEdge(edge);
				if (nextEdge == edge)
					break;
				edge = nextEdge;
			}
		}

		// If this is not the last vertex in the buffer, replace it with the
		// last (this is why the order is not maintained)
		int lastIndex = mVertexBuffer.size() - 1;
		Vertex lastVertex = mVertexBuffer.get(lastIndex);
		if (lastVertex != vertex) {
			int vertexIndex = vertex.index();
			mVertexBuffer.set(vertexIndex, lastVertex);
			lastVertex.setIndex(vertexIndex);
		}
		mVertexBuffer.remove(lastIndex);
	}

	public void deleteEdge(Edge edge) {
		Vertex sourceVertex = edge.sourceVertex();
		Vertex destVertex = edge.destVertex();

		sourceVertex.removeEdge(edge);
		destVertex.removeEdge(edge.dual());

		edge.addFlags(Edge.FLAG_DELETED);
		edge.dual().addFlags(Edge.FLAG_DELETED);
	}

	public String toString() {
		return dump(true, false);
	}

	public String dump(boolean withVertexLocations, boolean withVertexNames) {
		StringBuilder sb = new StringBuilder("Mesh:\n");
		for (Vertex v : mVertexBuffer) {
			sb.append(" ");
			if (withVertexNames) {
				sb.append(nameOf(v, false));
				sb.append(' ');
			}
			if (withVertexLocations) {
				sb.append(v.toStringAsInts());
			}

			Edge e = v.edges();
			if (e != null) {
				sb.append(" --> ");
				while (true) {
					Point dest = e.destVertex();
					if (withVertexNames) {
						sb.append(nameOf(dest, false));
						sb.append(' ');
					}
					if (withVertexLocations) {
						sb.append(dest.toStringAsInts());
					}
					e = e.nextEdge();
					if (e == v.edges())
						break;
					sb.append("   ");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public void render(AlgorithmStepper s) {
		final int COLOR_LIGHTBLUE = Color.argb(80, 100, 100, 255);
		s.setColor(COLOR_LIGHTBLUE);
		for (Edge e : constructListOfEdges(true)) {
			RenderTools.renderLine(e.sourceVertex(), e.destVertex());
		}
	}

	private void addEdgeToVertex(Edge edge, Vertex vertex) {
		if (vertex.edges() == null) {
			vertex.setEdges(edge);
			edge.setNextEdge(edge);
			edge.setPrevEdge(edge);
		} else {
			// Find insertion point for edge
			Edge existingEdge = vertex.edges();
			// Look for the existing edge that will immediately preceded this
			// one
			while (true) {
				float diff = edge.angle() - existingEdge.angle();

				if (diff < 0) {
					existingEdge = existingEdge.prevEdge();
					break;
				} else {
					Edge nextEdge = existingEdge.nextEdge();
					if (nextEdge == vertex.edges()) {
						break;
					}
					existingEdge = nextEdge;
				}
			}

			// It's a degeneracy if the angle between the new edge and its
			// neighbors is too close to zero or PI/2 ...
			Edge degenerateEdge = null;
			if (angleDifferenceIsDegenerate(edge.angle() - existingEdge.angle())) {
				degenerateEdge = existingEdge;
			}
			Edge followingEdge = existingEdge.nextEdge();
			if (followingEdge != existingEdge) {
				if (angleDifferenceIsDegenerate(followingEdge.angle()
						- edge.angle())) {
					degenerateEdge = followingEdge;
				}
			}

			if (degenerateEdge != null) {
				if (degenerateEdge.destVertex() == vertex) {
					GeometryException.raise("edge already exists: "
							+ degenerateEdge);
				} else {
					GeometryException.raise("edges collinear: " + edge + " "
							+ degenerateEdge + " " + edge.angle() + " "
							+ degenerateEdge.angle());
				}
			}

			insertEdgeAfter(edge, existingEdge);
			if (edge.angle() < existingEdge.angle()) {
				vertex.setEdges(edge);
			}
		}
	}

	private void insertEdgeAfter(Edge newEdge, Edge previousEdge) {
		Edge follower = previousEdge.nextEdge();
		previousEdge.setNextEdge(newEdge);
		newEdge.setPrevEdge(previousEdge);
		newEdge.setNextEdge(follower);
		follower.setPrevEdge(newEdge);
	}

	private boolean angleDifferenceIsDegenerate(float angleDiff) {
		final float kMinSeparation = 1e-7f;
		angleDiff = Math.abs(normalizePseudoAngle(angleDiff));
		return (angleDiff <= kMinSeparation || angleDiff >= PSEUDO_ANGLE_RANGE_12
				- kMinSeparation);
	}

	private List<Vertex> mVertexBuffer = new ArrayList();
}
