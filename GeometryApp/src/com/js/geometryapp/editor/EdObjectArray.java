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

	/**
	 * Invalidate any cached information in preparation for changes being made
	 * to this object
	 */
	private void prepareForChanges() {
		if (mFrozen)
			throw new IllegalStateException();
		mFrozenVersion = null;
		mSelectedSlots = null;
	}

	public boolean isEmpty() {
		return mList.isEmpty();
	}

	public EdObjectArray freeze() {
		if (!mFrozen) {
			mFrozen = true;
			mFrozenVersion = this;
		}
		return this;
	}

	public void clear() {
		prepareForChanges();
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
		prepareForChanges();
		int index = mList.size();
		mList.add(object);
		return index;
	}

	public <T extends EdObject> T get(int index) {
		return (T) mList.get(index);
	}

	public void set(int index, EdObject object) {
		prepareForChanges();
		mList.set(index, object);
	}

	public int size() {
		return mList.size();
	}

	/**
	 * Construct subset of this array, using particular slots only
	 * 
	 * @param slots
	 *            SlotList
	 */
	public EdObjectArray getSubset(List<Integer> slots) {
		EdObjectArray subset = new EdObjectArray();
		for (int slot : slots) {
			subset.add(get(slot));
		}
		return subset;
	}

	public EdObjectArray getMutableCopy() {
		EdObjectArray copy = new EdObjectArray();
		for (EdObject obj : mList)
			copy.add(obj);
		copy.mFrozenVersion = this.mFrozenVersion;
		return copy;
	}

	public EdObjectArray getFrozen() {
		return getCopy().freeze();
	}

	public EdObjectArray getCopy() {
		if (isFrozen())
			return this;
		return getMutableCopy();
	}

	public boolean isFrozen() {
		return mFrozen;
	}

	/**
	 * Get slots of selected items
	 */
	public List<Integer> getSelectedSlots() {
		if (mSelectedSlots == null) {
			List<Integer> slots = SlotList.build();
			for (int i = 0; i < mList.size(); i++) {
				if (mList.get(i).isSelected()) {
					slots.add(i);
				}
			}
			mSelectedSlots = slots;
		}
		return mSelectedSlots;
	}

	/**
	 * Make specific slots selected, and others unselected
	 */
	public void setSelected(List<Integer> slots) {
		prepareForChanges();
		int j = 0;
		for (int i = 0; i < mList.size(); i++) {
			boolean sel = j < slots.size() && slots.get(j) == i;
			mList.get(i).setSelected(sel);
			if (sel)
				j++;
		}
		mSelectedSlots = slots;
	}

	public void setEditableSlot(int slot) {
		prepareForChanges();
		unselectAll();
		if (slot >= 0) {
			EdObject editObject = get(slot);
			editObject.setEditable(true);
		}
	}

	public void removeSelected() {
		List<Integer> slots = getSelectedSlots();
		prepareForChanges();
		List<EdObject> newList = new ArrayList();
		int j = 0;
		for (int i = 0; i < mList.size(); i++) {
			if (j < slots.size() && i == slots.get(j)) {
				j++;
				continue;
			}
			newList.add(mList.get(i));
		}
		mList = newList;
	}

	/**
	 * Replace selected objects with copies
	 */
	public void replaceSelectedObjectsWithCopies() {
		prepareForChanges();
		for (int slot : getSelectedSlots()) {
			EdObject obj = get(slot);
			set(slot, obj.getCopy());
		}
	}

	public void unselectAll() {
		prepareForChanges();
		setSelected(SlotList.build());
	}

	public void selectAll() {
		prepareForChanges();
		for (EdObject obj : mList)
			obj.setSelected(true);
	}

	@Override
	public String toString() {
		if (!DEBUG_ONLY_FEATURES)
			return null;
		else {
			doNothing();
			StringBuilder sb = new StringBuilder("EdObjectArray");
			sb.append(" [");
			for (EdObject obj : mList) {
				sb.append(" " + obj.getFactory().getTag());
			}
			sb.append("]");
			return sb.toString();
		}
	}

	private List<EdObject> mList = new ArrayList();
	private boolean mFrozen;
	private EdObjectArray mFrozenVersion;
	private List<Integer> mSelectedSlots;
}
