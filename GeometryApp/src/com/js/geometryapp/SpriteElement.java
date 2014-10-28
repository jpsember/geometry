package com.js.geometryapp;

import com.js.geometry.Point;

class SpriteElement extends AlgorithmDisplayElement {

	public SpriteElement(int spriteResourceId, Point location) {
		mSpriteResourceId = spriteResourceId;
		mLocation = location;
	}

	@Override
	public void render(AlgorithmStepper s) {
		iconSet().plot(mSpriteResourceId, mLocation,
				AlgorithmDisplayElement.getRenderColor());
	}

	private int mSpriteResourceId;
	private Point mLocation;
}
