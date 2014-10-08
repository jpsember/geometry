package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.js.basic.Tools.*;

/**
 * An array of EdObjects, including utility methods
 */
public class EdObjectArray {

	/**
	 * Delete specific items
	 * 
	 * @param itemSlots
	 *            indexes of items to delete
	 * @deprecated not sure if needed yet
	 */
	void deleteItems(ArrayList<Integer> itemSlots) {
		sort(itemSlots);
		for (int i = itemSlots.size() - 1; i >= 0; i--) {
			int j = itemSlots.get(i);
			mList.remove(j);
		}
	}

	public void set(int index, EdObject object) {
		mList.set(index, object);
	}

	public int size() {
		return mList.size();
	}

	public EdObject get(int index) {
		return mList.get(index);
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

	/**
	 * Add items
	 * 
	 * @param itemSlots
	 *            final indexes of items
	 * @param items
	 *            items to add
	 * @deprecated not sure if needed yet
	 */
	void addItems(ArrayList<Integer> itemSlots, EdObjectArray items,
			boolean setSelected) {

		sort(itemSlots);
		for (int j : itemSlots) {
			EdObject obj = items.get(j);
			if (setSelected)
				obj.setSelected(true);

			mList.add(itemSlots.get(j), obj);
		}
	}

	public void unselectAll() {
		for (EdObject obj : mList) {
			obj.setSelected(false);
		}
	}

	/**
	 * Sort array of Integers into ascending order
	 */
	private static void sort(ArrayList<Integer> values) {
		Collections.sort(values, new Comparator() {
			@Override
			public int compare(Object i1, Object i2) {
				return ((Integer) i1 - (Integer) i2);
			}
		});
	}

	/**
	 * Get array of slot indexes
	 * 
	 * @param source
	 *            : ObjArray to examine
	 * @param selectedOnly
	 *            : if true, returns only slots of selected items; else, every
	 *            item (i.e. 0...n-1)
	 * @return DArray of slots
	 * @deprecated not sure if needed yet
	 */
	static ArrayList<Integer> getItemSlots(EdObjectFactory objType,
			ArrayList<EdObject> source, boolean selectedOnly,
			boolean skipInactive) {
		// Tools.warn("what to do about incomplete objects?");
		ArrayList<Integer> ret = new ArrayList();
		for (int i = 0; i < source.size(); i++) {
			EdObject obj = source.get(i);
			if (objType != null && obj.getFactory() != objType)
				continue;
			if (skipInactive && !obj.isActive())
				continue;
			if (selectedOnly && !obj.isSelected())
				continue;
			if (!obj.complete()) {
				warning("skipping incomplete objects");
				continue;
			}
			ret.add(i);
		}
		return ret;
	}

	/**
	 * Replace selected EditObjects within this array with those from another
	 * 
	 * @param source
	 *            : source ObjArray
	 * @param slots
	 *            : where to store each source object in this array
	 * @param setSelected
	 *            : if true, sets each object's highlighted flag
	 * @deprecated not sure if needed yet
	 */
	static void replaceSelectedObjects(ArrayList<EdObject> editorObjects,
			ArrayList<EdObject> source, ArrayList<Integer> slots,
			boolean setSelected) {
		for (int i = 0; i < slots.size(); i++) {
			EdObject obj = source.get(i);
			if (setSelected)
				obj.setSelected(true);
			editorObjects.set(slots.get(i), obj);
		}
	}

	private ArrayList<EdObject> mList = new ArrayList();

}
