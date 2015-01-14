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

  @Override
  public void mutate() {
    super.mutate();
    mSelectedSlots = null;
  }

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
   * Add an object to the end of the list
   * 
   * @param object
   * @return the index of the object
   */
  public int add(EdObject object) {
    mutate();
    int index = mList.size();
    mList.add(object);
    return index;
  }

  public <T extends EdObject> T get(int index) {
    return (T) mList.get(index);
  }

  public void set(int index, EdObject object) {
    mutate();
    mList.set(index, object);
  }

  public int size() {
    return mList.size();
  }

  /**
   * Construct array containing only the selected objects from this array
   */
  public EdObjectArray getSelectedObjects() {
    EdObjectArray subset = new EdObjectArray();
    for (int slot : getSelectedSlots()) {
      subset.add(get(slot));
    }
    return subset;
  }

  @Override
  public Freezable getMutableCopy() {
    EdObjectArray copy = new EdObjectArray();
    for (EdObject obj : mList)
      copy.add(obj);
    return copy;
  }

  /**
   * Get slots of selected items
   */
  public SlotList getSelectedSlots() {
    if (mSelectedSlots == null) {
      SlotList slots = new SlotList();
      for (int i = 0; i < mList.size(); i++) {
        if (mList.get(i).isSelected()) {
          slots.add(i);
        }
      }
      slots.freeze();
      setSelected(slots);
    }
    return mSelectedSlots;
  }

  /**
   * Make specific slots selected, and others unselected
   */
  public void setSelected(SlotList slots) {
    slots = frozen(slots);
    int j = 0;
    for (int i = 0; i < mList.size(); i++) {
      boolean sel = j < slots.size() && slots.get(j) == i;
      mList.get(i).setSelected(sel);
      if (sel)
        j++;
    }
    if (j != slots.size())
      throw new IllegalArgumentException();
    mSelectedSlots = slots;
  }

  public void setEditableSlot(int slot) {
    ASSERT(slot >= 0);
    setSelected(new SlotList(slot));
    get(slot).setEditable(true);
    mutate();
  }

  public void removeSelected() {
    SlotList slots = getSelectedSlots();
    mutate();
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
    for (EdObject obj : mList)
      obj.setSelected(true);
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

  private List<EdObject> mList = new ArrayList();
  private SlotList mSelectedSlots;
}
