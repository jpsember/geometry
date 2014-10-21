package com.js.geometryapp.editor;

import com.js.geometryapp.AlgorithmStepper;

public interface EditorEventListener {

	public EditorEvent processEvent(EditorEvent event);

	// Perform any rendering specific to this operation
	public void render(AlgorithmStepper s);
}
