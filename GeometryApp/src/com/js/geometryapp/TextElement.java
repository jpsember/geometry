package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;

class TextElement extends AlgorithmDisplayElement {

	public TextElement(String text, Point location) {
		mText = text;
		mLocation = location;
	}

	@Override
	public void render(AlgorithmStepper s) {
		setColorState(color());
		renderText(mLocation, mText);
	}

	private Point mLocation;
	private String mText;
}
