package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;
import com.js.geometry.Renderable;

class TextElement implements Renderable {

	public TextElement(String text, Point location) {
		mText = text;
		mLocation = location;
	}

	@Override
	public void render(AlgorithmStepper s) {
		RenderTools.renderText(mLocation, mText);
	}

	private Point mLocation;
	private String mText;
}
