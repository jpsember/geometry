package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import static com.js.basic.Tools.*;
import com.js.basic.Freezable;

/**
 * An array of EdObjects
 */
public class EdObjectArray extends Freezable.Mutable implements
    Iterable<EdObject> {

  public boolean isEmpty() {
    return mList.isEmpty();
  }

  public void clear() {
    mutate();
    mList.clear();
  }

  public Iterator<EdObject> iterator() {
    return mList.iterator();
  }

  /**
   * Freeze object and add it to the end of the list.
   */
  public int add(EdObject object) {
    mutate();
    object.freeze();
    int index = mList.size();
    mList.add(object);
    return index;
  }

  public <T extends EdObject> T get(int index) {
    return (T) mList.get(index);
  }

  /**
   * Freeze object and place it within the array, replacing existing object in
   * that slot
   */
  public void set(int slot, EdObject object) {
    mutate();
    object.freeze();
    mList.set(slot, object);
  }

  public int size() {
    return mList.size();
  }

  /**
   * Construct array containing only the selected objects from this array
   */
  public EdObjectArray getSelectedObjects() {
    return getSubset(getSelectedSlots());
  }

  /**
   * Construct subset of this array
   */
  public EdObjectArray getSubset(SlotList slots) {
    EdObjectArray subset = new EdObjectArray();
    for (int slot : slots) {
      subset.add(get(slot));
    }
    return subset;
  }

  public EdObjectArray() {
    this(null);
  }

  private EdObjectArray(EdObjectArray source) {
    if (source == null) {
      mList = new ArrayList();
      mSelectedSlots = new SlotList();
      mSelectedSlots.freeze();
      return;
    }
    for (EdObject obj : source.mList)
      add(obj);
    mEditableSlot = source.mEditableSlot;
    mSelectedSlots = source.mSelectedSlots;
  }

  @Override
  public Freezable getMutableCopy() {
    return new EdObjectArray(this);
  }

  /**
   * Get slots of selected items
   */
  public SlotList getSelectedSlots() {
    return mSelectedSlots;
  }

  private void setSelectedSlotsAux(SlotList slots) {
    mSelectedSlots = slots;
    mEditableSlot = -1;
    if (mSelectedSlots.size() == 1)
      mEditableSlot = mSelectedSlots.get(0);
  }

  /**
   * Make specific slots selected, and others unselected
   */
  public void setSelected(SlotList slots) {
    mutate();
    slots = frozen(slots);
    if (!slots.isEmpty() && slots.last() >= size())
      throw new IllegalArgumentException();
    setSelectedSlotsAux(slots);
  }

  public void setEditableSlot(int slot) {
    setSelected(new SlotList(slot));
  }

  public int getEditableSlot() {
    return mEditableSlot;
  }

  /**
   * Replace selected objects with copies
   * 
   * @deprecated
   */
  public void replaceSelectedObjectsWithCopies() {
    SlotList selectedSlots = getSelectedSlots();
    mutate();
    for (int slot : selectedSlots) {
      EdObject obj = get(slot);
      set(slot, copyOf(obj));
    }
  }

  public void unselectAll() {
    mutate();
    setSelected(new SlotList());
  }

  public void selectAll() {
    mutate();
    SlotList list = new SlotList();
    for (int i = 0; i < size(); i++)
      list.add(i);
    list.freeze();
    setSelected(list);
  }

  @Override
  public String toString() {
    if (!DEBUG_ONLY_FEATURES)
      return null;
    else {
      StringBuilder sb = new StringBuilder("EdObjectArray");
      sb.append(" [");
      for (EdObject obj : mList) {
        sb.append(" " + obj.getFactory().getTag());
      }
      sb.append("]");
      return sb.toString();
    }
  }

  public boolean isSlotEditable(int slot) {
    return slot == mEditableSlot;
  }

  public boolean isSlotSelected(int slot) {
    return mSelectedSlots.contains(slot);
  }

  private List<EdObject> mList = new ArrayList();
  private SlotList mSelectedSlots = new SlotList();
  private int mEditableSlot = -1;
}
