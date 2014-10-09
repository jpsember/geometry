package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.js.basic.Tools.*;

/**
 * An array of EdObjects, including utility methods; also optionally contains
 * sequence of slot numbers indicating the objects' positions within some other
 * list
 */
public class EdObjectArray implements Iterable<EdObject> {

	public boolean isEmpty() {
		return mList.isEmpty();
	}

	public void clear() {
		mList.clear();
	}

	public Iterator<EdObject> iterator() {
		return mList.iterator();
	}

	public void set(int index, EdObject object) {
		mList.set(index, object);
	}

	public int size() {
		return mList.size();
	}

	public <T extends EdObject> T get(int index) {
		return (T) mList.get(index);
	}

	public int getSlot(int index) {
		return mSlots.get(index);
	}

	public EdObjectArray getSubsequence(List<Integer> slots) {
		EdObjectArray subset = new EdObjectArray();
		int prevSlot = -1;
		for (int slot : slots) {
			ASSERT(slot > prevSlot);
			subset.add(get(slot));
			prevSlot = slot;
		}
		subset.setSlots(slots);
		return subset;
	}

	/**
	 * Utility method to construct a copy of a list of slots
	 */
	public static List<Integer> copyOf(List<Integer> slots) {
		return new ArrayList(slots);
	}

	/**
	 * Construct subset containing those objects that are selected
	 */
	public EdObjectArray getSelected() {
		List<Integer> slots = new ArrayList();
		for (int i = 0; i < mList.size(); i++) {
			if (mList.get(i).isSelected()) {
				slots.add(i);
			}
		}
		return getSubsequence(slots);
	}

	/**
	 * Make specific slots selected, and others unselected
	 */
	public void select(List<Integer> slots) {
		int j = 0;
		for (int i = 0; i < mList.size(); i++) {
			boolean sel = j < slots.size() && slots.get(j) == i;
			mList.get(i).setSelected(sel);
			if (sel)
				j++;
		}
	}

	/**
	 * Get subsequence of this array
	 * 
	 * @param slots
	 *            sequence of slots to include
	 * @return
	 */
	public EdObjectArray get(List<Integer> slots) {
		EdObjectArray subsequence = new EdObjectArray();
		for (int slot : slots) {
			subsequence.add(get(slot));
		}
		return subsequence;
	}

	/**
	 * Replace selected objects with copies
	 * 
	 * @param slots
	 *            sequence of slots to include
	 * @return array containing original objects
	 */
	public EdObjectArray replaceWithCopies(List<Integer> slots) {
		EdObjectArray subsequence = new EdObjectArray();
		for (int slot : slots) {
			EdObject obj = get(slot);
			subsequence.add(obj);
			set(slot, (EdObject) obj.clone());
		}
		return subsequence;
	}

	/**
	 * Add an object to the end of the list
	 * 
	 * @param object
	 * @return the index of the object
	 */
	public int add(EdObject object) {
		int index = mList.size();
		mList.add(object);
		return index;
	}

	public void unselectAll() {
		for (EdObject obj : mList) {
			obj.setSelected(false);
		}
	}

	public void setSlots(List<Integer> slots) {
		mSlots = slots;
	}

	public List<Integer> getSlots() {
		return mSlots;
	}

	public void replace(List<Integer> slots, EdObjectArray replacementObjects) {
		ASSERT(slots.size() == replacementObjects.size());
		for (int i = 0; i < slots.size(); i++) {
			set(slots.get(i), replacementObjects.get(i));
		}
	}

	private ArrayList<EdObject> mList = new ArrayList();

	// If not null, a list of slots corresponding to the objects in this array,
	// indicating their positions within some other array
	private List<Integer> mSlots;
}
