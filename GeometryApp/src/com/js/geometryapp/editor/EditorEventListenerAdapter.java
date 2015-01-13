package com.js.geometryapp.editor;

import com.js.geometry.AlgorithmStepper;

@Deprecated
public abstract class EditorEventListenerAdapter implements EditorEventListener {
	@Override
	public abstract EditorEvent processEvent(EditorEvent event);

	@Override
	public void render(AlgorithmStepper s) {
	}

	@Override
	public boolean allowEditableObject() {
		return true;
	}

}
