package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import com.js.editor.UserOperation;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.basic.Rect;
import com.js.geometryapp.ConcreteStepper;

public class UnhideOperation extends UserOperation.InstantOperation {

  public UnhideOperation(Editor editor, ConcreteStepper stepper) {
    mEditor = editor;
    mStepper = stepper;
  }

  @Override
  public boolean shouldBeEnabled() {
    findHiddenObjects();
    return !mSlots.isEmpty();
  }

  private void findHiddenObjects() {
    mSlots = findHiddenObjects(mEditor.objects(), null);
  }

  @Override
  public void start() {
    if (!shouldBeEnabled())
      return;
    CommandForGeneralChanges command = new CommandForGeneralChanges(mEditor,
        null, "Unhide");

    EdObjectArray objects = mEditor.objects();
    objects.setSelected(mSlots);
    for (int slot : mSlots) {
      EdObject orig = objects.get(slot);
      EdObject obj = mutableCopyOf(orig);
      obj.moveBy(orig, mCorrectingTranslation);
      objects.set(slot, obj);
    }
    command.finish();
  }

  /**
   * Find which objects, if any, are essentially offscreen and thus hidden from
   * the user.
   * 
   * @param objects
   *          list of objects to examine
   * @param translationToApply
   *          if not null, simulates applying this translation before performing
   *          offscreen test
   * @return slots of hidden objects
   */
  public SlotList findHiddenObjects(EdObjectArray objects,
      Point translationToApply) {
    float minUnhideSquaredDistance = 0;
    boolean translationDefined = false;

    Rect r = mStepper.visibleRect();
    if (translationToApply != null) {
      r = new Rect(r);
      r.translate(-translationToApply.x, -translationToApply.y);
    }

    // Construct a slightly inset version for detecting hidden objects, and
    // a more inset one representing where we'll move vertices to unhide
    // them
    Rect outerRect = new Rect(r);
    float inset = mEditor.pickRadius() * 2;
    outerRect.inset(inset, inset);
    Rect innerRect = new Rect(r);
    innerRect.inset(inset * 2, inset * 2);

    mCorrectingTranslation = null;
    SlotList slots = new SlotList();
    for (int i = 0; i < objects.size(); i++) {
      EdObject obj = objects.get(i);
      if (obj.intersects(outerRect))
        continue;

      // This is a hidden object; add to output list
      slots.add(i);

      // See if one of its vertices is closest yet to the inner rect
      for (int j = 0; j < obj.nPoints(); j++) {
        Point v = obj.getPoint(j);
        Point v2 = outerRect.nearestPointTo(v);
        float squaredDistance = MyMath.squaredDistanceBetween(v, v2);
        if (!translationDefined || squaredDistance < minUnhideSquaredDistance) {
          translationDefined = true;
          mCorrectingTranslation = MyMath.subtract(v2, v);
          minUnhideSquaredDistance = squaredDistance;
        }
      }
    }
    return slots;
  }

  /**
   * When preceded by a call to findHiddenObjects(), returns a translation that
   * if applied to all offscreen objects will bring at least one of them
   * onscreen. Returns null if no offscreen objects were found
   */
  public Point getCorrectingTranslation() {
    return mCorrectingTranslation;
  }

  private SlotList mSlots;
  private Editor mEditor;
  private ConcreteStepper mStepper;
  private Point mCorrectingTranslation;
}
