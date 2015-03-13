package com.js.geometryapp;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.android.MyActivity;
import com.js.android.ResolutionInfo;
import com.js.geometry.AlgorithmStepper;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonMesh;
import com.js.geometry.R;
import com.js.basic.Rect;
import com.js.geometry.Renderable;
import com.js.opengl.Font;
import com.js.opengl.GLTools;
import com.js.opengl.SpriteSet;

import static com.js.basic.Tools.*;

/**
 * Utility methods and global state for algorithm-related rendering in OpenGL
 */
public class RenderTools {

	public static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

	// If true, plots algorithm rect in white on gray background, vs entire view
	// in white
	public static final boolean SHOW_ALG_RECT = false;

	public static void clearView(GL10 gl) {
		if (SHOW_ALG_RECT) {
			// Clear the entire OpenGL view to a gray
			final float GRAY = .8f;
			gl.glClearColor(GRAY, GRAY, GRAY, 1f);
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			// Fill the algorithm bounds with white
			sPolygonProgram.setColor(Color.WHITE);
			sPolygonProgram.render(sAlgBoundsMesh);
		} else {
			gl.glClearColor(1, 1, 1, 1);
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		}
	}

	static int getElementCount() {
		int r = sElementsConstructed;
		sElementsConstructed = 0;
		return r;
	}

	public static void renderLine(float x1, float y1, float x2, float y2) {
		renderLine(new Point(x1, y1), new Point(x2, y2));
	}

	public static void renderLine(Point p1, Point p2) {
		sPolylineProgram.setColor(getRenderColor());
		sPolylineVertices.clear();
		sPolylineVertices.add(p1);
		sPolylineVertices.add(p2);
		sPolylineProgram.render(sPolylineVertices, null, null, false);
	}

	public static void renderRay(Point p1, Point p2) {
		float length = MyMath.distanceBetween(p1, p2);
		if (length < .2f)
			return;
		float angleOfRay = MyMath.polarAngleOfSegment(p1, p2);
		float setback = sArrowheadLength * .3f;
		boolean drawArrowhead = (length > 2 * setback);
		Point p2b = p2;
		if (drawArrowhead) {
			p2b = MyMath
					.interpolateBetween(p1, p2, (length - setback) / length);
			Matrix m = buildRotationMatrix(angleOfRay);
			m.postConcat(buildTranslationMatrix(p2));
			sPolygonProgram.setColor(sColor);
			sPolygonProgram.render(sArrowheadMesh, null, m);
		}
		renderLine(p1, p2b);
	}

	static void setLineWidthState(float lineWidth) {
		sLineWidth = lineWidth;
	}

	static void setColorState(int color) {
		sColor = color;
	}

	public static void renderPoint(Point point) {
		renderPoint(point, 1);
	}

	public static void renderPoint(Point point, float radius) {
		Matrix matrix = null;
		Point translation = null;
		// If no scaling required, we can just use a translation vector
		if (radius == 1) {
			translation = point;
		} else {
			Matrix translationMatrix = buildTranslationMatrix(point);
			matrix = buildScaleMatrix(radius);
			matrix.postConcat(translationMatrix);
		}
		sPolygonProgram.setColor(sColor);
		sPolygonProgram.render(sPointMesh, translation, matrix);
	}

	private static ResolutionInfo resolutionInfo() {
		return MyActivity.getResolutionInfo();
	}

	/**
	 * Have the algorithm stepper display elements prepare to use an OpenGL
	 * renderer; i.e. setting up shaders and fonts. Should be called within
	 * onSurfaceCreated(...)
	 */
	static void setRenderer(Rect algorithmBounds, AlgorithmRenderer renderer) {
		GLTools.ensureRenderThread();
		sRenderer = renderer;
		sPolylineProgram = new PolylineProgram(renderer,
				AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		sPolygonProgram = new PolygonProgram(renderer,
				AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		if (SHOW_ALG_RECT)
			buildAlgBounds(algorithmBounds);
		buildArrowheads();
		buildPoints();
		prepareSprites();

		int titleFontSize = 18;
		int elementFontSize = 14;
		if (false) {
			warning("using unusually large fonts");
			titleFontSize = 35;
			elementFontSize = 30;
		}
		sTitleFont = new Font(
				(int) (titleFontSize * resolutionInfo().density()));
		sElementFont = new Font((int) (elementFontSize * resolutionInfo()
				.density()));

		resetRenderStateVars();
	}

	public static void renderText(Point location, String text) {
		Point p = new Point(location);

		// Convert location from algorithm space to view space
		Matrix matrix = sRenderer
				.getTransform(AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_DEVICE);
		p.apply(matrix);

		Font font = sElementFont;

		// Place text centered vertically, to the right of the given point
		p.y += font.lineHeight() / 2;
		p.x += font.characterWidth();

		font.setColor(sColor);
		font.render(text, p);
	}

	private static void splitLongLines(String text, int maxLineLength,
			ArrayList<String> destination) {

		// Prefix to add to substring if it followed a split point
		String splitPrefix = "    ";

		// Keep max line length to something reasonable
		maxLineLength = Math.max(2 + splitPrefix.length(), maxLineLength);

		// The minimum size of line resulting from splitting on space,
		// vs splitting at arbitrary location
		int minSubstringLength = (int) (maxLineLength * .5f);

		int i = 0; // the cursor position
		while (i != text.length()) {
			int maxThisLineLength = maxLineLength;
			if (i != 0)
				maxThisLineLength -= splitPrefix.length();

			// Extract next substring. Set j to the cursor position for the next
			// iteration
			int j = Math.min(text.length(), i + maxThisLineLength);
			if (j < text.length()) {
				// determine if splitting at space is practical
				int spaceLocation = i + text.substring(i, j).lastIndexOf(' ');
				if (spaceLocation < i + minSubstringLength) {
					spaceLocation = j;
				}
				j = spaceLocation;
			}
			String textPortion = text.substring(i, j);
			if (i != 0) {
				textPortion = splitPrefix + textPortion;
			}
			destination.add(textPortion);

			// Advance cursor past extracted text portion, and any following
			// spaces
			i = j;

			while (i < text.length() && text.charAt(i) == ' ')
				i++;
		}
		// If input was empty, always add single empty string
		if (i == 0)
			destination.add("");
	}

	/**
	 * Split a string into lines that will fit within the algorithm view
	 */
	private static void splitStringIntoLines(String text, int maxLineWidth,
			ArrayList<String> destination) {
		destination.clear();
		String[] lines = text.split("\\n");
		for (String s : lines) {
			splitLongLines(s, maxLineWidth, destination);
		}
	}

	static void renderFrameTitle(String sFrameTitle) {
		int titleIndentPixels = 10;

		ArrayList<String> lines = new ArrayList();
		Font font = sTitleFont;
		for (int pass = 0; pass < 2; pass++) {
			if (pass != 0)
				font = sElementFont;
			// Determine maximum length of displayed line
			int maxLineWidth = (int) ((sRenderer.deviceSize().x - 2 * titleIndentPixels) / font
					.characterWidth());
			splitStringIntoLines(sFrameTitle, maxLineWidth, lines);
			// Don't do a second pass if at most four lines generated
			if (lines.size() <= 4)
				break;
		}

		Point textLocation = new Point(titleIndentPixels, 10
				+ font.lineHeight() * lines.size());
		font.setColor(Color.BLACK);
		for (String s : lines) {
			font.render(s, textLocation);
			textLocation.y -= font.lineHeight();
		}
	}

	private static Matrix buildScaleMatrix(float scale) {
		Matrix m = new Matrix();
		m.setScale(scale, scale);
		return m;
	}

	private static Matrix buildTranslationMatrix(Point p) {
		Matrix m = new Matrix();
		m.setTranslate(p.x, p.y);
		return m;
	}

	private static Matrix buildRotationMatrix(float radians) {
		Matrix m = new Matrix();
		m.setRotate(radians / MyMath.M_DEG);
		return m;
	}

	private static void buildArrowheads() {
		sArrowheadLength = resolutionInfo().inchesToPixelsAlgorithm(.15f);

		Polygon p = Polygon.polygonWithScript("0 0 -1 .4 -1 -.4");
		p.apply(buildScaleMatrix(sArrowheadLength));
		sArrowheadMesh = PolygonMesh.meshForConvexPolygon(p);
	}

	private static void buildAlgBounds(Rect algorithmBounds) {
		Polygon p = new Polygon();
		for (int i = 0; i < 4; i++)
			p.add(algorithmBounds.corner(i));
		sAlgBoundsMesh = PolygonMesh.meshForConvexPolygon(p);
	}

	private static void buildPoints() {
		int POINT_VERTICES = 10;
		Polygon p = Polygon.circleWithOrigin(Point.ZERO, resolutionInfo()
				.inchesToPixelsAlgorithm(.025f), POINT_VERTICES);
		sPointMesh = PolygonMesh.meshForConvexPolygon(p);
	}

	public static PolygonProgram polygonProgram() {
		return sPolygonProgram;
	}

	public static PolylineProgram polylineProgram() {
		return sPolylineProgram;
	}

	/**
	 * Reset the global render attributes to their default values
	 */
	static void resetRenderStateVars() {
		sLineWidth = 1.0f;
		sColor = Color.BLUE;
	}

	public static int getRenderColor() {
		return sColor;
	}

	static float getRenderLineWidth() {
		return sLineWidth;
	}

	private static final int[] sSpriteIds = { //
	//
			R.raw.crosshairicon, //
			R.raw.squareicon, //
	};

	private static void prepareSprites() {
		sIcons = new SpriteSet(sRenderer.context(), sSpriteIds);
		sIcons.setTransformName(AlgorithmRenderer.TRANSFORM_NAME_ALGORITHM_TO_NDC);
		sIcons.compile();
	}

	public static SpriteSet iconSet() {
		return sIcons;
	}

	/**
	 * Wrap a Renderable in an object that also stores the current render state
	 * (color, line width)
	 */
	static Renderable wrapRenderableWithState(Renderable r) {
		// If already wrapped, leave unchanged
		if (r instanceof RenderableStateWrapper)
			return r;
		return new RenderableStateWrapper(r, getRenderColor(),
				getRenderLineWidth());
	}

	private static class RenderableStateWrapper implements Renderable {
		public RenderableStateWrapper(Renderable r, int color, float lineWidth) {
			mRenderable = r;
			mColor = color;
			mLineWidth = lineWidth;
		}

		@Override
		public void render(AlgorithmStepper stepper) {
			stepper.setColor(mColor);
			stepper.setLineWidth(mLineWidth);
			mRenderable.render(stepper);
		}

		private Renderable mRenderable;
		private int mColor;
		private float mLineWidth;
	}

	/**
	 * Construct a wrapper for a Renderable that renders it with a particular
	 * color
	 */
	static Renderable colored(final int color, final Renderable r) {
		return new RenderableStateWrapper(r, color, getRenderLineWidth());
	}

	private static float sArrowheadLength;
	private static PolygonProgram sPolygonProgram;
	private static PolylineProgram sPolylineProgram;
	private static PolygonMesh sArrowheadMesh;
	private static PolygonMesh sAlgBoundsMesh;
	private static PolygonMesh sPointMesh;
	private static Font sTitleFont;
	private static Font sElementFont;
	private static List<Point> sPolylineVertices = new ArrayList();
	private static float sLineWidth;
	private static int sColor;
	private static int sElementsConstructed;
	private static AlgorithmRenderer sRenderer;
	private static SpriteSet sIcons;
}
