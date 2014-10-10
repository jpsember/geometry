package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions for constructing and manipulating lists of slots, which are
 * associated with EdObjectArrays. These are actually implemented as
 * List<Integer>s.
 */
public class SlotList {

	/**
	 * Construct an empty slot list
	 */
	public static List<Integer> build() {
		return build(-1);
	}

	/**
	 * Construct a slot list, optionally with a single slot
	 * 
	 * @param initialSlot
	 *            if >= 0, this slot is added
	 */
	public static List<Integer> build(int initialSlot) {
		List slots = new ArrayList();
		if (initialSlot >= 0)
			slots.add(initialSlot);
		return slots;
	}

}
