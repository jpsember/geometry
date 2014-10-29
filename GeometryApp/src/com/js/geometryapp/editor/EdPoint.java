package com.js.geometryapp.editor;

import android.graphics.Color;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;

public class EdPoint extends EdObject {

	private EdPoint() {
	}

	private Point location() {
		return getPoint(0);
	}

	@Override
	public boolean valid() {
		return nPoints() == 1;
	}

	@Override
	public float distFrom(Point pt) {
		return MyMath.distanceBetween(pt, location());
	}

	@Override
	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	@Override
	public EditorEventListener buildEditOperation(int slot, Point location) {
		// Points are special in that their entire object is represented by a
		// single vertex; hence editing a point is equivalent to moving it
		// around. No per-vertex editing is required, and should in fact
		// be disallowed to keep the move and DupAccumulator logic simple.
		return null;
	}

	@Override
	public void render(AlgorithmStepper s) {
		if (isSelected()) {
			super.render(s);
		} else {
			s.setColor(Color.BLUE);
			s.plot(getPoint(0));
		}
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("p") {
		@Override
		public EdObject construct(Point defaultLocation) {
			EdPoint pt = new EdPoint();
			if (defaultLocation != null)
				pt.addPoint(defaultLocation);
			return pt;
		}


		@Override
		public int getIconResource() {
			return R.raw.pointicon;
		}
	};

}
