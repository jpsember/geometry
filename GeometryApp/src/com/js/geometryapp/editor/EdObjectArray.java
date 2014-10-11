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

	public <T extends EdObject> T get(int index) {
		return (T) mList.get(index);
	}

	public void set(int index, EdObject object) {
		mList.set(index, object);
	}

	public int size() {
		return mList.size();
	}

	public int getSlot(int index) {
		return mSlots.get(index);
	}

	/**
	 * Replace objects within particular slots
	 * 
	 * @param slots
	 *            slots of objects to replace
	 * @param replacementObjects
	 *            array of replacements; size must match slots
	 * @param allowAppending
	 *            if true, allows appending items to end of existing array
	 */
	public void replace(List<Integer> slots, EdObjectArray replacementObjects,
			boolean allowAppending) {
		if (slots.size() != replacementObjects.size())
			throw new IllegalArgumentException();
		for (int i = 0; i < slots.size(); i++) {
			int slot = slots.get(i);
			EdObject object = replacementObjects.get(i);
			if (allowAppending && slot == size())
				add(object);
			else
				set(slot, object);
		}
	}

	/**
	 * Construct subset of this array, using particular slots only
	 * 
	 * @param slots
	 *            SlotList
	 */
	public EdObjectArray getSubset(List<Integer> slots) {
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
	 * Construct subset of this array that consists of a single slot
	 */
	public EdObjectArray getSubset(int slot) {
		return getSubset(SlotList.build(slot));
	}

	/**
	 * Get slots of selected items
	 */
	public List<Integer> getSelectedSlots() {
		List<Integer> slots = SlotList.build();
		for (int i = 0; i < mList.size(); i++) {
			if (mList.get(i).isSelected()) {
				slots.add(i);
			}
		}
		return slots;
	}

	/**
	 * Make specific slots selected, and others unselected
	 */
	public void selectOnly(List<Integer> slots) {
		int j = 0;
		for (int i = 0; i < mList.size(); i++) {
			boolean sel = j < slots.size() && slots.get(j) == i;
			mList.get(i).setSelected(sel);
			if (sel)
				j++;
		}
	}

	public void remove(List<Integer> slots) {
		ArrayList<EdObject> newList = new ArrayList();
		int j = 0;
		for (int i = 0; i < mList.size(); i++) {
			if (j < slots.size() && i == slots.get(j)) {
				j++;
				continue;
			}
			newList.add(mList.get(i));
		}
		mList = newList;
		mSlots = null;
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

	public void unselectAll() {
		selectOnly(new ArrayList());
	}

	public void setSlots(List<Integer> slots) {
		mSlots = slots;
	}

	public List<Integer> getSlots() {
		return mSlots;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("EdObjectArray");
		if (mSlots != null)
			sb.append(" slots" + d(mSlots));
		sb.append(" items[");
		for (EdObject obj : mList) {
			sb.append(" " + obj.getFactory().getTag());
		}
		sb.append("]");
		return sb.toString();
	}

	private ArrayList<EdObject> mList = new ArrayList();

	// If not null, a list of slots corresponding to the objects in this array,
	// indicating their positions within some other array
	private List<Integer> mSlots;
}
