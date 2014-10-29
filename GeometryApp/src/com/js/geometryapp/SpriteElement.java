package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;
import com.js.geometry.Renderable;

class SpriteElement implements Renderable {

	public SpriteElement(int spriteResourceId, Point location) {
		mSpriteResourceId = spriteResourceId;
		mLocation = location;
	}

	@Override
	public void render(AlgorithmStepper s) {
		RenderTools.iconSet().plot(mSpriteResourceId, mLocation,
				RenderTools.getRenderColor());
	}

	private int mSpriteResourceId;
	private Point mLocation;
}
