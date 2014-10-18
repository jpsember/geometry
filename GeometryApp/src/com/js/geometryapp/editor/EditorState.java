package com.js.geometryapp.editor;

import java.util.List;

/**
 * Encapsulates the state of an editor, including all entities that are mutable
 * and need to be saved and restored to support undo/redo operations
 */
public class EditorState {

	public EditorState(Editor e) {
		mObjects = e.objects().getFrozen();
		mSelectedSlots = mObjects.getSelectedSlots();
		mClipboard = e.getClipboard();
		mDupAccumulator = new DupAccumulator(e.getDupAccumulator());
	}

	public EdObjectArray getObjects() {
		return mObjects;
	}

	public EdObjectArray getClipboard() {
		return mClipboard;
	}

	public List<Integer> getSelectedSlots() {
		return mSelectedSlots;
	}

	public DupAccumulator getDupAccumulator() {
		return mDupAccumulator;
	}

	private EdObjectArray mObjects;
	private List<Integer> mSelectedSlots;
	private EdObjectArray mClipboard;
	private DupAccumulator mDupAccumulator;
}
