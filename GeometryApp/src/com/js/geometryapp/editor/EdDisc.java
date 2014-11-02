package com.js.geometryapp.editor;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Disc;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.geometry.Rect;

import static com.js.basic.Tools.*;

public class EdDisc extends EdObject {

	private EdDisc() {
	}

	@Override
	public boolean valid() {
		return nPoints() == 2;
	}

	@Override
	public Rect getBounds(boolean ignoreSelectedFlag) {
		// The bounds is not just the smallest rect containing the two points,
		// but is instead the square containing the disc they represent
		float radius = getRadius();
		Point origin = getOrigin();
		if (!ignoreSelectedFlag && isSelected())
			radius += editor().pickRadius();
		Rect r = new Rect(origin.x - radius, origin.y - radius, radius * 2,
				radius * 2);
		return r;
	}

	@Override
	public void setPoint(int index, Point point) {
		// We must interpret a new second point as adjusting the radius
		if (index != 1)
			super.setPoint(index, point);
		else
			setRadius(MyMath.distanceBetween(getOrigin(), point));
	}

	private Point calculateSecondaryPointForRadius(float r) {
		return MyMath.pointOnCircle(getOrigin(), MyMath.M_DEG * 90, r);
	}

	@Override
	public void render(AlgorithmStepper s) {
		s.setColor(Color.BLUE);
		s.plot(buildDisc());
		super.render(s);
	}

	@Override
	public EditorEventListener buildEditOperation(int slot, Point location) {
		int vertexIndex = closestVertex(location, editor().pickRadius());
		if (vertexIndex >= 0)
			return new EditorOperation(editor(), slot, vertexIndex);
		return null;
	}

	public Disc buildDisc() {
		return new Disc(getOrigin(), getRadius());
	}

	public float distFrom(Point pt) {
		Point p1 = getPoint(0);
		float dist = MyMath.distanceBetween(pt, p1);
		float radius = getRadius();
		float dist2 = Math.abs(dist - radius);
		dist = Math.min(dist, dist2);
		return dist;
	}

	public float getRadius() {
		return getPoint(1).y - getOrigin().y;
	}

	private void setRadius(float r) {
		super.setPoint(1, calculateSecondaryPointForRadius(r));
	}

	public Point getOrigin() {
		return getPoint(0);
	}

	private void setOrigin(Point p) {
		setPoint(0, p);
	}

	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	@Override
	public void applyTransform(Matrix m) {
		float radius = getRadius();
		Point origin = getOrigin();

		// Transform the left and bottom sides of the bounding square
		Point bottomLeft = new Point(origin.x - radius, origin.y - radius);
		Point bottomRight = new Point(origin.x + radius, bottomLeft.y);
		Point topLeft = new Point(bottomLeft.x, origin.y + radius);

		bottomLeft.apply(m);
		bottomRight.apply(m);
		topLeft.apply(m);

		// The new origin is the midpoint of the diagonal of the transformed
		// square
		Point newOrigin = MyMath.interpolateBetween(bottomRight, topLeft, .5f);
		// The new radius is the smaller of the distances from the origin to the
		// transformed sides
		float bottomDistance = MyMath.ptDistanceToLine(newOrigin, bottomLeft,
				bottomRight, null);
		float leftDistance = MyMath.ptDistanceToLine(newOrigin, bottomLeft,
				topLeft, null);
		float newRadius = Math.min(bottomDistance, leftDistance);
		setOrigin(newOrigin);
		setRadius(newRadius);
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("d") {
		@Override
		public EdObject construct(Point defaultLocation) {
			EdDisc s = new EdDisc();
			if (defaultLocation != null) {
				s.setOrigin(defaultLocation);
				s.setRadius(20);
			}
			return s;
		}

		@Override
		public int getIconResource() {
			return R.raw.discicon;
		}

		@Override
		public void parsePoints(EdObject destinationObject,
				org.json.JSONObject map) throws org.json.JSONException {
			EdDisc disc = (EdDisc) destinationObject;
			super.parsePoints(disc, map);
			if (disc.nPoints() == 2) {
				disc.setRadius(MyMath.distanceBetween(disc.getOrigin(),
						disc.getPoint(1)));
			}
		}

	};

	private static class EditorOperation extends EditorEventListenerAdapter {
		public EditorOperation(Editor editor, int slot, int vertexNumber) {
			mEditor = editor;
			mEditSlot = slot;
			mEditPointIndex = vertexNumber;
		}

		/**
		 * Initialize the edit operation, if it hasn't already been
		 * 
		 * This is necessary because we may start the operation without an
		 * EVENT_DOWN_x
		 */
		private void initializeOperation(Point location) {
			if (mOriginalState != null)
				return;
			mOriginalState = new EditorState(mEditor);
		}

		@Override
		public EditorEvent processEvent(EditorEvent event) {
			final boolean db = false && DEBUG_ONLY_FEATURES;
			if (db)
				event.printProcessingMessage("EdDisc");

			if (event.hasLocation())
				initializeOperation(event.getLocation());

			// By default, we'll be handling the event
			EditorEvent outputEvent = EditorEvent.NONE;

			switch (event.getCode()) {
			default:
				// we don't know how to handle this event, so pass it
				// through
				outputEvent = event;
				break;

			case EditorEvent.CODE_DOWN:
				mOriginalDisc = mEditor.objects().get(mEditSlot);
				mInitialOffset = MyMath.subtract(
						mOriginalDisc.getPoint(mEditPointIndex),
						event.getLocation());

				break;

			case EditorEvent.CODE_DRAG: {
				Point adjustedLoc = MyMath.add(event.getLocation(),
						mInitialOffset);
				EdDisc disc = mEditor.objects().get(mEditSlot);
				// Create a new copy of the disc, with modified origin or radius
				EdDisc disc2 = disc.getCopy();
				if (mEditPointIndex == 1) {
					disc2.setRadius(MyMath.distanceBetween(adjustedLoc,
							mOriginalDisc.getOrigin()));
				} else {
					disc2.setOrigin(adjustedLoc);
					disc2.setRadius(disc.getRadius());
				}
				mEditor.objects().set(mEditSlot, disc2);
				mModified = true;
			}
				break;

			case EditorEvent.CODE_UP:
				// stop the operation on UP events
				outputEvent = EditorEvent.STOP;
				if (event.isMultipleTouch())
					break;
				if (mModified) {
					mEditor.pushCommand(Command.constructForGeneralChanges(
							mOriginalState, new EditorState(mEditor),
							FACTORY.getTag()));
				}
				break;
			}
			return outputEvent;
		}

		private int mEditSlot;
		private int mEditPointIndex;
		private EdDisc mOriginalDisc;
		private Point mInitialOffset;
		private boolean mModified;
		private EditorState mOriginalState;
		private Editor mEditor;
	}
}
