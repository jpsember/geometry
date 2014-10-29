package com.js.geometry;

import com.js.geometryapp.RenderTools;

public class Text implements Renderable {

	public Text(String text, Point location) {
		mText = new String(text);
		mLocation = new Point(location);
	}

	@Override
	public void render(AlgorithmStepper s) {
		RenderTools.renderText(mLocation, mText);
	}

	private String mText;
	private Point mLocation;
}
