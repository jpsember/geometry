package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.android.AppPreferences;
import com.js.android.MyActivity;
import com.js.android.QuiescentDelayOperation;
import com.js.android.TouchEventGenerator;
import com.js.android.UITools;
import com.js.basic.JSONTools;
import com.js.editor.Command;
import com.js.editor.UserEvent;
import com.js.editor.UserEventManager;
import com.js.editor.UserEventSource;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Disc;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.R;
import com.js.geometry.Rect;
import com.js.geometry.Sprite;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.ConcreteStepper;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.AbstractWidget.Listener;
import com.js.geometryapp.widget.CheckBoxWidget;
import com.js.geometryapp.widget.TextWidget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import static com.js.basic.Tools.*;
import static com.js.android.Tools.*;

/**
 * Class that encapsulates editing geometric objects. It includes a view, which
 * contains both a content view to display the objects being edited, as well as
 * floating toolbars.
 */
public class Editor {

  private static final boolean ZAP_SUPPORTED = (true && DEBUG_ONLY_FEATURES);
  private static final boolean DB_SNAPSHOT = (false && DEBUG_ONLY_FEATURES);

  private static final boolean DB_JSON = false && DEBUG_ONLY_FEATURES;
  private static final int MAX_COMMAND_HISTORY_SIZE = 30;
  private static final String JSON_KEY_OBJECTS = "obj";
  private static final String JSON_KEY_CLIPBOARD = "cb";
  private static final String JSON_KEY_FILENAME = "filename";
  private static final int MAX_OBJECTS_IN_FILE = 500;

  public void setDependencies(GeometryStepperActivity activity,
      ConcreteStepper stepper, AlgorithmOptions options,
      AlgorithmRenderer renderer) {
    mActivity = activity;
    mStepper = stepper;
    mOptions = options;
    mRenderer = renderer;
  }

  public AlgorithmStepper stepper() {
    return mStepper;
  }

  /**
   * @param contentView
   *          view displaying objects being edited; probably a GLSurfaceView
   */
  public void prepare(View contentView) {
    warning("rotate operation now misbehaves");
    disposeOfStateSnapshot();
    mState = new EditorState();
    mEditorView = contentView;
    prepareObjectTypes();
    mUserEventManager = new UserEventManager(new DefaultUserOperation(this,
        mStepper));

    mTouchEventGenerator = new TouchEventGenerator();
    mTouchEventGenerator.setView(new UserEventSource() {
      @Override
      public Point viewToWorld(Point viewPt) {
        // Transform point from device to algorithm coordinates
        Matrix deviceToAlgorithMatrix = mRenderer
            .getTransform(AlgorithmRenderer.TRANSFORM_NAME_DEVICE_TO_ALGORITHM);
        Point worldPt = new Point(viewPt);
        worldPt.apply(deviceToAlgorithMatrix);
        return worldPt;
      }

      @Override
      public UserEventManager getManager() {
        return mUserEventManager;
      }
    }, mEditorView);

    mUserEventManager.setListener(new UserEvent.Listener() {
      @Override
      public void processUserEvent(UserEvent event) {
        updatePlotTouchLocation(event);
        // Request a refresh of the editor view after any event
        refresh();
      }
    });
    mUserEventManager.setEnabled(true);
  }

  private TouchEventGenerator mTouchEventGenerator;

  /**
   * Construct the various editor widgets
   * 
   * @param algorithmOptions
   */
  public void prepareOptions() {

    // Place these controls in the aux controls view
    mOptions.pushView(getAuxControlsView());

    // Add a horizontal row of buttons
    {
      mOptions.pushView(mOptions.addView(false));
      mOptions.addButton("Undo", "icon", R.raw.undoicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doUndo();
              refresh();
            }
          });
      mOptions.addButton("Redo", "icon", R.raw.redoicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doRedo();
              refresh();
            }
          });
      mOptions.addButton("Cut", "icon", R.raw.cuticon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doCut();
              refresh();
            }
          });
      mOptions.addButton("Copy", "icon", R.raw.copyicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doCopy();
              refresh();
            }
          });

      mOptions.addButton("Paste", "icon", R.raw.pasteicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doPaste();
              refresh();
            }
          });
      mOptions.addButton("Dup", "icon", R.raw.duplicateicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doDup();
              refresh();
            }
          });
      mOptions.popView();
    }
    {
      mOptions.pushView(mOptions.addView(false));

      prepareAddObjectButtons("Pt", EdPoint.FACTORY, //
          "Seg", EdSegment.FACTORY, //
          "Disc", EdDisc.FACTORY,//
          "Poly", EdPolyline.FACTORY);

      mOptions.addButton("Scale", "icon", R.raw.scaleicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doScale();
              refresh();
            }
          });
      mOptions.addButton("Rotate", "icon", R.raw.rotateicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doRotate();
              refresh();
            }
          });

      mOptions.popView();
    }
    mOptions.popView();

    // put additional controls in the options window

    mFilenameWidget = mOptions.addEditText(AlgorithmOptions.WIDGET_ID_FILENAME,
        "label", "Filename", "editable", true);
    mFilenameWidget.setValidator(new AbstractWidget.Validator() {
      public String validate(AbstractWidget widget, String value) {
        return sanitizeFilename(value);
      }
    });
    mRenderAlways = mOptions.addCheckBox("_render_always_", "label",
        "Always plot editor");
    mOptions.addStaticText("");

    mOptions.pushView(mOptions.addView(false));
    {
      mOptions.addButton("All", "icon", R.raw.allicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doSelectAll();
              refresh();
            }
          });
      mOptions.addButton("Unhide", "icon", R.raw.unhideicon).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doUnhide();
              refresh();
            }
          });
      if (ZAP_SUPPORTED) {
        mOptions.addButton("Zap").addListener(new Listener() {
          public void valueChanged(AbstractWidget widget) {
            doZap();
            refresh();
          }
        });
      }
      mOptions.addButton("Share").addListener(new Listener() {
        public void valueChanged(AbstractWidget widget) {
          doShare();
        }
      });

    }
    mOptions.popView();
  }

  private void doShare() {
    try {
      JSONObject map = compileObjectsToJSON();
      String filename = mFilenameWidget.getValue();
      map.put(JSON_KEY_FILENAME, filename);
      String jsonState = map.toString();
      byte[] bytes = jsonState.toString().getBytes();
      mActivity.doShare(filename, bytes);
    } catch (JSONException e) {
      showException(context(), e, null);
    }
  }

  private void prepareAddObjectButtons(Object... args) {
    int i = 0;
    while (i < args.length) {
      String label = (String) args[i];
      final EdObjectFactory factory = (EdObjectFactory) args[i + 1];

      mOptions.addButton(label, "icon", factory.getIconResource()).addListener(
          new Listener() {
            public void valueChanged(AbstractWidget widget) {
              doStartAddObjectOperation(factory);
              refresh();
            }
          });
      i += 2;
    }
  }

  /**
   * Get the view displaying the editor; construct if necessary. This contains
   * the contentView
   */
  public View getView() {
    return mEditorView;
  }

  public boolean isActive() {
    return mOptions.isEditorActive();
  }

  /**
   * Render editor-related elements to the contentView; this includes all the
   * EdObjects, and any highlighting related to an active operation (e.g. a
   * selection rectangle)
   */
  public void render() {
    // Calculate the pick radius always, because we may use it even if the
    // editor is not active
    mPickRadius = MyActivity.getResolutionInfo().inchesToPixelsAlgorithm(.14f);

    if (!isActive() && !mRenderAlways.getBooleanValue())
      return;
    mStepper.setRendering(true);

    EdObjectArray mObjects = objects();
    int editableSlot = mObjects.getEditableSlot();
    SlotList sel = mObjects.getSelectedSlots();
    int selCursor = 0;
    for (int slot = 0; slot < mObjects.size(); slot++) {
      EdObject obj = mObjects.get(slot);
      boolean selected = false;
      if (selCursor < sel.size() && sel.get(selCursor) == slot) {
        selCursor++;
        selected = true;
      }
      obj.render(mStepper, selected, editableSlot == slot);
    }
    unimp("maybe we need to pass mStepper to paint()");
    mUserEventManager.getOperation().paint();
    if (mPlotTouchLocation != null) {
      mStepper.setColor(Color.BLACK);
      mStepper.render(new Sprite(R.raw.crosshairicon, mPlotTouchLocation));
    }
    mStepper.setRendering(false);
  }

  private void updatePlotTouchLocation(UserEvent event) {
    if (event.isUpVariant()) {
      mPlotTouchLocation = null;
    } else if (event.hasLocation()) {
      mPlotTouchLocation = event.getWorldLocation();
    }
  }

  /**
   * Make an object editable if it is the only selected object (and current
   * operation allows editable highlighting). We perform this operation with
   * each refresh, since this is simpler than trying to maintain the editable
   * state while the editor objects undergo various editing operations. Also,
   * update the last editable object type to reflect the editable object (if one
   * exists)
   */
  private void updateEditableObjectStatus() {
    boolean allowEditableObject = mUserEventManager.getOperation()
        .allowEditableObject();
    EdObject editableObject = null;

    EdObjectArray items = objects();
    SlotList list = items.getSelectedSlots();
    if (list.size() == 1 && allowEditableObject) {
      int newEditable = list.get(0);
      editableObject = items.get(newEditable);
    }
    if (editableObject != null)
      mLastEditableObjectType = editableObject.getFactory();
  }

  public void refresh() {
    updateEditableObjectStatus();

    mStepper.refresh();
    // Set delay to save changes
    persistEditorState(true);
    updateButtonEnableStates();
  }

  private void updateButtonEnableStates() {
    if (!mOptions.isEditorActive())
      return;
    if (QuiescentDelayOperation.replaceExisting(mPendingEnableOperation)) {
      final float ENABLE_DELAY = .1f;
      mPendingEnableOperation = new QuiescentDelayOperation("enable",
          ENABLE_DELAY, new Runnable() {
            public void run() {
              updateButtonEnableStatesAux();
            }
          });
    }
  }

  private void updateButtonEnableStatesAux() {
    SlotList selected = objects().getSelectedSlots();
    mOptions.setEnabled("Undo", mCommandHistoryCursor > 0);
    mOptions.setEnabled("Redo", mCommandHistoryCursor < mCommandHistory.size());
    mOptions.setEnabled("Cut", !selected.isEmpty());
    mOptions.setEnabled("Copy", !selected.isEmpty());
    mOptions.setEnabled("Paste", !clipboard().isEmpty());
    mOptions.setEnabled("Dup", !selected.isEmpty());
    mOptions.setEnabled("All", selected.size() < objects().size());
    mOptions.setEnabled("Unhide", unhidePossible());
    mOptions.setEnabled("Scale", !selected.isEmpty());
    mOptions.setEnabled("Rotate", !selected.isEmpty());
  }

  /**
   * Restore the editor state, including the EdObjects, from a JSON string
   */
  public void restoreFromJSON(String script) {
    if (DB_JSON) {
      pr("\n\nRestoring JSON:\n" + script);
    }
    try {
      JSONObject map = JSONTools.parseMap(script);
      if (!map.has(JSON_KEY_OBJECTS)) {
        throw new JSONException(JSON_KEY_OBJECTS + " key missing");
      }
      EdObjectArray objects = new EdObjectArray();
      EdObjectArray clipboard = new EdObjectArray();
      parseObjects(map, JSON_KEY_OBJECTS, objects);
      parseObjects(map, JSON_KEY_CLIPBOARD, clipboard);
      mFilenameWidget.setValue(map.optString(JSON_KEY_FILENAME));
      disposeOfStateSnapshot();
      mState = new EditorState(objects, clipboard, null);
    } catch (JSONException e) {
      showException(context(), e, "Problem parsing json");
    }
  }

  /**
   * Parse an EdObjectArray from JSON map, if found
   * 
   * @param map
   * @param key
   *          key objects are stored as
   * @param objectsArray
   *          where to store the objects; cleared beforehand if key found
   * @return object array, or null if no key found
   * @throws JSONException
   */
  private void parseObjects(JSONObject map, String key,
      EdObjectArray objectsArray) throws JSONException {
    if (!map.has(key))
      return;
    objectsArray.clear();

    JSONArray array = map.getJSONArray(key);
    int effectiveArrayLength = array.length();
    if (!verifyObjectsAllowed(effectiveArrayLength)) {
      effectiveArrayLength = Math
          .min(effectiveArrayLength, MAX_OBJECTS_IN_FILE);
    }

    for (int i = 0; i < effectiveArrayLength; i++) {
      JSONObject objMap = array.getJSONObject(i);
      String tag = objMap.getString(EdObjectFactory.JSON_KEY_TYPE);
      EdObjectFactory factory = mObjectTypes.get(tag);
      if (factory == null) {
        warning("no factory found for: " + tag);
        continue;
      }
      EdObject edObject = factory.parse(objMap);
      edObject.setEditor(this);
      if (!edObject.valid()) {
        warning("Unable to parse: " + objMap);
        continue;
      }
      objectsArray.add(edObject);
    }
  }

  /**
   * Save the editor state (including EdObjects) to a JSON string within the
   * user preferences
   * 
   * @param withDelay
   *          if true, save operation is delayed by several seconds using the
   *          QuiescentDelayOperation
   */
  public void persistEditorState(boolean withDelay) {
    if (!withDelay) {
      persistEditorStateAux();
      return;
    }
    // Make a delayed call to persist the values
    if (QuiescentDelayOperation.replaceExisting(mPendingFlushOperation)) {
      final float FLUSH_DELAY = 5.0f;
      mPendingFlushOperation = new QuiescentDelayOperation("flush editor",
          FLUSH_DELAY, new Runnable() {
            public void run() {
              persistEditorStateAux();
            }
          });
    }
  }

  /**
   * Get the EdObjectArray being edited
   */
  EdObjectArray objects() {
    return mState.getObjects();
  }

  private Context context() {
    return mEditorView.getContext();
  }

  /**
   * Set pending operation to add a new object of a particular type
   */
  public void doStartAddObjectOperation(EdObjectFactory objectType) {
    objects().unselectAll();

    if (!verifyObjectsAllowed(objects().size() + 1)) {
      return;
    }

    mUserEventManager.setOperation(new AddObjectOperation(this, objectType));
    if (false) {
      toast(context(), "Add " + objectType.getTag());
    }
  }

  private void persistEditorStateAux() {
    try {
      JSONObject map = compileObjectsToJSON();
      String jsonState = map.toString();
      if (!jsonState.equals(mLastSavedState)) {
        AppPreferences.putString(GeometryStepperActivity.PERSIST_KEY_EDITOR,
            jsonState);
        mLastSavedState = jsonState;
        if (DB_JSON) {
          pr("\n\nJSON:\n" + mLastSavedState);
        }
      }
    } catch (JSONException e) {
      showException(context(), e, null);
    }
  }

  private JSONArray getEdObjectsArrayJSON(EdObjectArray objects)
      throws JSONException {
    JSONArray values = new JSONArray();

    for (EdObject obj : objects) {
      values.put(obj.getFactory().write(obj));
    }
    if (DB_JSON) {
      pr("constructed JSONArray " + values);
    }
    return values;
  }

  private JSONObject compileObjectsToJSON() throws JSONException {
    JSONObject editorMap = new JSONObject();
    editorMap.put(JSON_KEY_OBJECTS, getEdObjectsArrayJSON(objects()));
    editorMap.put(JSON_KEY_CLIPBOARD, getEdObjectsArrayJSON(clipboard()));
    return editorMap;
  }

  private void addObjectType(EdObjectFactory factory) {
    mObjectTypes.put(factory.getTag(), factory);
  }

  private void prepareObjectTypes() {
    mObjectTypes = new HashMap();
    addObjectType(EdPoint.FACTORY);
    addObjectType(EdSegment.FACTORY);
    addObjectType(EdDisc.FACTORY);
    addObjectType(EdPolyline.FACTORY);
  }

  private void doUndo() {
    // Button enabling is delayed, so we can't assume this operation is
    // possible
    if (mCommandHistoryCursor == 0) {
      return;
    }
    mCommandHistoryCursor--;
    Command command = mCommandHistory.get(mCommandHistoryCursor);
    pr("undoing " + command);
    command.getReverse().perform();
  }

  private void doRedo() {
    // Button enabling is delayed, so we can't assume this operation is
    // possible
    if (mCommandHistoryCursor == mCommandHistory.size()) {
      return;
    }
    Command command = mCommandHistory.get(mCommandHistoryCursor);
    command.perform();
    mCommandHistoryCursor++;
  }

  private void doCut() {
    CommandForGeneralChanges c = new CommandForGeneralChanges(this, null, "Cut");
    if (c.getOriginalState().getSelectedSlots().isEmpty())
      return;

    EdObjectArray objects = objects();
    SlotList allSlots = SlotList.buildComplete(objects.size());
    SlotList selectedSlots = objects.getSelectedSlots();
    SlotList newSlots = allSlots.minus(selectedSlots);

    EdObjectArray newObjects = objects.getSubset(newSlots);
    EdObjectArray newClipboard = objects.getSubset(selectedSlots);

    mState.setObjects(newObjects);
    mState.setClipboard(newClipboard);

    resetDuplicationOffset();
    c.finish();
  }

  private void doCopy() {
    CommandForGeneralChanges c = new CommandForGeneralChanges(this, null,
        "Copy");
    if (c.getOriginalState().getSelectedSlots().isEmpty())
      return;
    EdObjectArray newClipboard = freeze(objects().getSelectedObjects());
    setClipboard(newClipboard);
    resetDuplicationOffset();
    c.finish();
  }

  /**
   * Determine if the current file can contain a particular number of objects.
   * If not, display a warning to the user and return false
   * 
   * @param requestedCapacity
   *          desired number of objects after user's operation is to be
   *          performed
   * 
   * @return true if requested capacity can be satisfied
   */
  private boolean verifyObjectsAllowed(int requestedCapacity) {
    if (requestedCapacity <= MAX_OBJECTS_IN_FILE)
      return true;
    toast(context(), "Too many objects!", Toast.LENGTH_LONG);
    return false;
  }

  private void doPaste() {
    CommandForGeneralChanges command = new CommandForGeneralChanges(this, null,
        "Paste");
    EdObjectArray mClipboard = command.getOriginalState().getClipboard();
    if (mClipboard.isEmpty())
      return;
    if (!verifyObjectsAllowed(objects().size() + mClipboard.size()))
      return;
    SlotList newSelected = new SlotList();

    mDupAffectsClipboard = true;
    adjustDupAccumulatorForPendingOperation(mClipboard);

    Point offset = getDupAccumulator();

    for (EdObject obj : mClipboard) {
      newSelected.add(objects().size());
      EdObject copy = mutableCopyOf(obj);
      copy.moveBy(obj, offset);
      objects().add(copy);
    }
    objects().setSelected(newSelected);

    replaceClipboardWithSelectedObjects();
    command.finish();
  }

  private void replaceClipboardWithSelectedObjects() {
    setClipboard(objects().getSelectedObjects());
  }

  private void doZap() {
    if (ZAP_SUPPORTED) {
      disposeOfStateSnapshot();
      mState = new EditorState();
      mCommandHistory.clear();
      mCommandHistoryCursor = 0;
    }
  }

  /**
   * Determine if items to be duplicated or pasted will remain on screen with
   * current duplication accumulator. If not, start with a fresh one
   * 
   * @param affectedObjects
   *          objects to be duplicated/pasted
   * @param affectsClipboard
   *          DupAccumulator construction argument
   */
  private void adjustDupAccumulatorForPendingOperation(
      EdObjectArray affectedObjects) {
    if (affectedObjects.size() == 0)
      return;
    Point offset = getDupAccumulator();
    Point correction = new Point();
    SlotList hiddenObjects = findHiddenObjects(affectedObjects, offset,
        correction);
    // If ALL the objects will end up being hidden, reset the
    // accumulator
    if (hiddenObjects.size() == affectedObjects.size()) {
      resetDuplicationOffsetWithCorrectingTranslation(correction);
    }
  }

  private void doDup() {
    CommandForGeneralChanges command = new CommandForGeneralChanges(this, null,
        "Duplicate");
    EditorState state = command.getOriginalState();
    if (state.getSelectedSlots().isEmpty())
      return;
    if (!verifyObjectsAllowed(objects().size()
        + state.getSelectedSlots().size()))
      return;

    mDupAffectsClipboard = false;
    adjustDupAccumulatorForPendingOperation(objects().getSelectedObjects());
    SlotList newSelected = new SlotList();

    Point offset = getDupAccumulator();

    for (int slot : state.getSelectedSlots()) {
      EdObject obj = objects().get(slot);
      EdObject copy = mutableCopyOf(obj);
      copy.moveBy(obj, offset);
      newSelected.add(objects().add(copy));
    }
    objects().setSelected(newSelected);
    command.finish();
  }

  private void resetDuplicationOffsetWithCorrectingTranslation(Point t) {
    resetDuplicationOffset();
    float angle = MyMath.polarAngle(t);
    // Calculate nearest cardinal angle
    final float CARDINAL_RANGE = MyMath.PI / 2;
    angle = MyMath.normalizeAngle(angle + CARDINAL_RANGE / 2);
    angle -= MyMath.myMod(angle, CARDINAL_RANGE);
    mState.setDupAccumulator(MyMath.pointOnCircle(Point.ZERO, angle,
        pickRadius()));
  }

  private Point getDupAccumulator() {
    Point accum = mState.getDupAccumulator();
    if (accum.magnitude() == 0) {
      accum = MyMath.pointOnCircle(Point.ZERO, 0, pickRadius());
    }
    return accum;
  }

  void resetDuplicationOffset() {
    mState.setDupAccumulator(null);
  }

  /**
   * Determine which slot, if any, holds the (at most one) editable object
   * 
   * @return slot if found, or -1
   */
  public int getEditableSlot() {
    return objects().getEditableSlot();
  }

  private void doScale() {
    if (objects().getSelectedSlots().isEmpty())
      return;
    mUserEventManager.setOperation(new ScaleOperation(this));
  }

  private void doRotate() {
    if (objects().getSelectedSlots().isEmpty())
      return;
    mUserEventManager.setOperation(new RotateOperation(this));
  }

  private boolean unhidePossible() {
    return !findHiddenObjects(objects(), null, null).isEmpty();
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
   * @param correctingTranslation
   *          if not null, and hidden objects are found, this is set to a
   *          translation to apply to all objects to bring (at least one of
   *          them) onscreen
   * @return slots of hidden objects
   */
  private SlotList findHiddenObjects(EdObjectArray objects,
      Point translationToApply, Point correctingTranslation) {
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
    float inset = pickRadius() * 2;
    outerRect.inset(inset, inset);
    Rect innerRect = new Rect(r);
    innerRect.inset(inset * 2, inset * 2);

    SlotList slots = new SlotList();
    for (int i = 0; i < objects.size(); i++) {
      EdObject obj = objects.get(i);
      if (obj.intersects(outerRect))
        continue;

      // This is a hidden object; add to output list
      slots.add(i);

      if (correctingTranslation != null) {
        // See if one of its vertices is closest yet to the inner rect
        for (int j = 0; j < obj.nPoints(); j++) {
          Point v = obj.getPoint(j);
          Point v2 = outerRect.nearestPointTo(v);
          float squaredDistance = MyMath.squaredDistanceBetween(v, v2);
          if (!translationDefined || squaredDistance < minUnhideSquaredDistance) {
            translationDefined = true;
            correctingTranslation.setTo(MyMath.subtract(v2, v));
            minUnhideSquaredDistance = squaredDistance;
          }
        }
      }
    }
    return slots;
  }

  private void doSelectAll() {
    objects().selectAll();
  }

  private void doUnhide() {
    CommandForGeneralChanges command = new CommandForGeneralChanges(this,
        "unhide", "Unhide");
    Point translation = new Point();
    SlotList slots = findHiddenObjects(objects(), null, translation);
    if (slots.isEmpty())
      return;

    objects().setSelected(slots);
    for (int slot : slots) {
      EdObject orig = objects().get(slot);
      EdObject obj = mutableCopyOf(orig);
      obj.moveBy(orig, translation);
      objects().set(slot, obj);
    }
    command.finish();
  }

  /**
   * Add a command that has already been performed to the undo stack
   */
  void pushCommand(Command command) {
    pr("pushing command " + command);
    // Throw out any older 'redoable' commands that will now be stale
    while (mCommandHistory.size() > mCommandHistoryCursor) {
      pop(mCommandHistory);
    }

    // Merge this command with its predecessor if possible
    while (true) {
      if (mCommandHistoryCursor == 0)
        break;
      Command prev = mCommandHistory.get(mCommandHistoryCursor - 1);
      Command merged = prev.attemptMergeWith(command);
      if (merged == null)
        break;
      pop(mCommandHistory);
      mCommandHistoryCursor--;
      command = merged;
    }

    mCommandHistory.add(command);
    mCommandHistoryCursor++;

    // If this command is not reversible, throw out all commands, including
    // this one
    if (command.getReverse() == null) {
      mCommandHistory.clear();
      mCommandHistoryCursor = 0;
    }

    if (mCommandHistoryCursor > MAX_COMMAND_HISTORY_SIZE) {
      int del = mCommandHistoryCursor - MAX_COMMAND_HISTORY_SIZE;
      mCommandHistoryCursor -= del;
      mCommandHistory.subList(0, del).clear();
    }

    // Dispose of any cached state snapshot; it's likely invalid since we just
    // performed a command
    disposeOfStateSnapshot();
  }

  public float pickRadius() {
    return mPickRadius;
  }

  public void expandRectByPickRadius(Rect rect) {
    float r = pickRadius();
    rect.inset(-r, -r);
  }

  public LinearLayout getAuxControlsView() {
    if (mAuxView == null) {
      mAuxView = UITools.linearLayout(context(), true);
    }
    return mAuxView;
  }

  public EdObjectArray clipboard() {
    return mState.getClipboard();
  }

  private void setClipboard(EdObjectArray clipboard) {
    mState.setClipboard(clipboard);
  }

  public AlgorithmInput constructAlgorithmInput() {
    AlgorithmInput algorithmInput = new AlgorithmInput(mStepper.algorithmRect());
    List<Point> pointList = new ArrayList();
    List<Polygon> polygonList = new ArrayList();
    List<Disc> discList = new ArrayList();

    for (EdObject obj : objects()) {
      if (obj instanceof EdPoint) {
        EdPoint pt = (EdPoint) obj;
        pointList.add(new Point(pt.getPoint(0)));
      } else if (obj instanceof EdPolyline) {
        EdPolyline polyline = (EdPolyline) obj;
        if (!polyline.closed())
          continue;
        Polygon polygon = new Polygon();
        for (int i = 0; i < polyline.nPoints(); i++)
          polygon.add(new Point(polyline.getPoint(i)));
        if (polygon.numVertices() < 3)
          continue;
        int orientation = polygon.orientation();
        if (orientation == 0)
          continue;
        if (orientation < 0)
          polygon.reverse();

        polygonList.add(polygon);
      } else if (obj instanceof EdDisc) {
        EdDisc disc = (EdDisc) obj;
        discList.add(new Disc(disc.getOrigin(), disc.getRadius()));
      }
    }
    algorithmInput.points = pointList.toArray(new Point[0]);
    algorithmInput.polygons = polygonList.toArray(new Polygon[0]);
    algorithmInput.discs = discList.toArray(new Disc[0]);
    return algorithmInput;
  }

  EditorState getStateSnapshot() {
    if (mStateSnapshot == null) {
      mStateSnapshot = frozen(mState);
      if (DB_SNAPSHOT) {
        pr("creating state snapshot:     " + nameOf(mStateSnapshot));
      }
    }
    return mStateSnapshot;
  }

  void disposeOfStateSnapshot() {
    if (DB_SNAPSHOT) {
      if (mStateSnapshot != null)
        pr("disposing of state snapshot: " + nameOf(mStateSnapshot));
    }
    mStateSnapshot = null;
  }

  void setState(EditorState state) {
    if (DB_SNAPSHOT) {
      pr("setState to:                   " + nameOf(state));
    }
    if (state.isMutable())
      throw new IllegalArgumentException();
    disposeOfStateSnapshot();
    mStateSnapshot = state;
    mState = mutableCopyOf(state);
  }

  /**
   * Adjust duplication accumulator (if one exists) by adding an additional
   * translation
   */
  void updateDupAccumulatorForTranslation(Point translation) {
    Point dup = mState.getDupAccumulator();
    mState.setDupAccumulator(MyMath.add(dup, translation));
    if (mDupAffectsClipboard)
      replaceClipboardWithSelectedObjects();
  }

  /**
   * Sanitize a user-specified filename. It should NOT include any path
   * information or an extension
   * 
   * @param name
   * @return sanitized name (which may be empty)
   */
  public String sanitizeFilename(String name) {
    Pattern p = Pattern.compile("[a-zA-Z_0-9 ]*");
    name = name.trim();
    Matcher m = p.matcher(name);
    if (!m.matches() || name.length() > 64) {
      name = "";
    }
    return name;
  }

  public EdObjectFactory getLastEditableObjectType() {
    return mLastEditableObjectType;
  }

  private UserEventManager mUserEventManager;
  private Map<String, EdObjectFactory> mObjectTypes;
  private EdObjectFactory mLastEditableObjectType;
  private View mEditorView;
  private GeometryStepperActivity mActivity;
  private ConcreteStepper mStepper;
  private QuiescentDelayOperation mPendingFlushOperation;
  private String mLastSavedState;
  private List<Command> mCommandHistory = new ArrayList();
  private int mCommandHistoryCursor;
  private float mPickRadius;
  private Point mPlotTouchLocation;
  private AlgorithmOptions mOptions;
  private AlgorithmRenderer mRenderer;
  private LinearLayout mAuxView;
  private QuiescentDelayOperation mPendingEnableOperation;
  private CheckBoxWidget mRenderAlways;
  private boolean mDupAffectsClipboard;
  // We keep a reference to this widget, since it isn't in the primary group
  // and thus may not be accessible via getWidget()
  private TextWidget mFilenameWidget;
  // The current (and mutable) editor state
  private EditorState mState;
  // The most recent frozen snapshot of the editor state
  private EditorState mStateSnapshot;
}
