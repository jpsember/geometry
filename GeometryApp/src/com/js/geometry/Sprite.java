package com.js.geometry;

import com.js.geometry.AlgorithmStepper;
import com.js.basic.Point;
import com.js.geometry.Renderable;
import com.js.geometryapp.RenderTools;

public class Sprite implements Renderable {

	public Sprite(int spriteResourceId, Point location) {
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
