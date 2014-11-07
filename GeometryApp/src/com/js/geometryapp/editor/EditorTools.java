package com.js.geometryapp.editor;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;

public class EditorTools {

	public static void plotRect(AlgorithmStepper s, Rect r) {
		Polygon p = new Polygon();
		for (int i = 0; i < 4; i++)
			p.add(r.corner(i));
		s.render(p);
	}
}
