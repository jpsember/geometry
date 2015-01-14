package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.Iterator;
import com.js.basic.Freezable;
import static com.js.basic.Tools.*;

/**
 * Utility functions for constructing and manipulating lists of slots, which are
 * associated with EdObjectArrays. These are actually implemented as
 * List<Integer>s.
 */
public class SlotList extends Freezable.Mutable implements Iterable<Integer> {

  @Override
  public Iterator<Integer> iterator() {
    return mList.iterator();
  }

  @Override
  public Freezable getMutableCopy() {
    return new SlotList(this);
  }

  public SlotList() {
    this(null);
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Build a list containing a single slot
   */
  public SlotList(int singleSlot) {
    this(null);
    add(singleSlot);
  }

  public void add(int slotNumber) {
    mutate();
    if (!isEmpty() && last() >= slotNumber)
      throw new IllegalArgumentException("not strictly increasing");
    mList.add(slotNumber);
  }

  public int size() {
    return mList.size();
  }

  public Integer last() {
    return com.js.basic.Tools.last(mList);
  }

  public Integer get(int index) {
    return mList.get(index);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SlotList ");
    sb.append(d(mList));
    return sb.toString();
  }

  private SlotList(SlotList source) {
    if (source == null)
      return;
    mList.addAll(source.mList);
  }

  private ArrayList<Integer> mList = new ArrayList();

}
