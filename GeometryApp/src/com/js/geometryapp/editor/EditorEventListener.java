package com.js.geometryapp.editor;

import com.js.geometry.AlgorithmStepper;

public interface EditorEventListener {

	/**
	 * Process an event, if possible
	 * 
	 * @param event
	 */
	public EditorEvent processEvent(EditorEvent event);

	// Perform any rendering specific to this operation
	public void render(AlgorithmStepper s);
}
