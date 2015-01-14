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
    if (slotNumber < 0 || (!isEmpty() && last() >= slotNumber))
      throw new IllegalArgumentException("not strictly increasing nonnegative");
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

  public boolean contains(int slot) {
    // Later: use binary search to determine this
    return mList.contains(slot);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SlotList ");
    sb.append(d(mList));
    return sb.toString();
  }

  public static SlotList intersection(SlotList a, SlotList b) {
    SlotList outputList = new SlotList();
    int cursor1 = 0;
    int cursor2 = 0;
    while (cursor1 < a.size() && cursor2 < b.size()) {
      int s1 = a.get(cursor1);
      int s2 = b.get(cursor2);
      if (s1 == s2)
        outputList.add(s1);
      if (s1 <= s2)
        cursor1++;
      if (s2 <= s1)
        cursor2++;
    }
    return outputList;
  }

  private SlotList(SlotList source) {
    if (source == null)
      return;
    mList.addAll(source.mList);
  }

  private ArrayList<Integer> mList = new ArrayList();

}
