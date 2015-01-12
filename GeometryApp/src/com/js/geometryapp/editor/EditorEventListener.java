package com.js.geometryapp.editor;

import com.js.geometry.AlgorithmStepper;

/**
 * @deprecated use com.js.editor package
 */
public interface EditorEventListener {

	/**
	 * Process an event, if possible
	 * 
	 * @param event
	 * @return event after filtering
	 * 
	 */
	public EditorEvent processEvent(EditorEvent event);

	/**
	 * Perform any rendering specific to this operation
	 */
	public void render(AlgorithmStepper s);

	/**
	 * Determine if an object can be editable during this operation
	 */
	public boolean allowEditableObject();
}
