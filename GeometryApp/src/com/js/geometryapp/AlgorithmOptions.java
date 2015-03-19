package com.js.geometryapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.js.android.AppPreferences;
import com.js.android.MyActivity;
import com.js.android.QuiescentDelayOperation;
import com.js.basic.JSONTools;
import com.js.geometryapp.editor.Editor;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.ButtonWidget;
import com.js.geometryapp.widget.CheckBoxWidget;
import com.js.geometryapp.widget.ComboBoxWidget;
import com.js.geometryapp.widget.SliderWidget;
import com.js.geometryapp.widget.AbstractWidget.Listener;
import com.js.geometryapp.widget.TextWidget;

import static com.js.android.Tools.*;
import static com.js.basic.Tools.*;
import static com.js.android.UITools.*;

public class AlgorithmOptions {

  static final String WIDGET_ID_TOTALSTEPS = "_steps_";
  static final String WIDGET_ID_TARGETSTEP = "_target_";
  public static final String WIDGET_ID_FILENAME = "_filename_";

  private static final String WIDGET_ID_PREV_OPER = "_prevoper_";

  // Algorithm-specific versions of the target & total steps
  private static final String WIDGET_ID_TOTALSTEPS_AUX = "_"
      + WIDGET_ID_TOTALSTEPS;
  private static final String WIDGET_ID_TARGETSTEP_AUX = "_"
      + WIDGET_ID_TARGETSTEP;

  private static final String WIDGET_ID_OPERATION = "_algorithm_";

  AlgorithmOptions(Context context) {
    mContext = context;
  }

  void setDependencies(Editor editor, ConcreteStepper stepper) {
    mEditor = editor;
    mStepper = stepper;
  }

  /**
   * Prepare the options views
   * 
   * @param containingView
   *          the view into which the option views should be placed
   */
  void prepareViews(LinearLayout containingView) {
    mContainingView = containingView;
    mPrimaryWidgetGroup = new WidgetGroup(getContext(), false);
    mContainingView.addView(mPrimaryWidgetGroup.getOuterContainer(),
        layoutParams(mContainingView, 0));
  }

  private void addPrimaryWidgets() {
    addSlider(WIDGET_ID_PREV_OPER, AbstractWidget.OPTION_HIDDEN, true);

    addButton("_<<", "label", "<").addListener(new Listener() {
      public void valueChanged(AbstractWidget widget) {
        int k = getIntValue(WIDGET_ID_PREV_OPER);
        if (k != mAlgorithms.indexOf(mActiveAlgorithm)) {
          setValue(WIDGET_ID_OPERATION, k);
        }
      }
    });

    ComboBoxWidget w = addComboBox(WIDGET_ID_OPERATION, "label", "Operation");
    for (ActiveOperationRecord r : mAlgorithms) {
      w.addItem(r.name());
    }
    w.prepare();
    w.addListener(new Listener() {
      @Override
      public void valueChanged(AbstractWidget widget) {
        int index = widget.getIntValue();
        selectAlgorithm(mAlgorithms.get(index));
      }
    });

    // Add some vertical space by adding blank text; perhaps later we'll add
    // widgets for this purpose
    addStaticText("");
  }

  private void prepareEditorOperationRecord() {
    ActiveOperationRecord algorithmRecord = new ActiveOperationRecord();
    mAlgorithms.add(algorithmRecord);
    algorithmRecord.setWidgetGroup(new WidgetGroup(getContext(), true));
    activateSecondaryWidgetGroup(algorithmRecord);
    mEditor.prepareOptions();
  }

  /**
   * Add a slider widget
   */
  public SliderWidget addSlider(String id, Object... attributePairs) {
    Map<String, Object> attributes = buildAttributes(id, attributePairs);
    SliderWidget w = new SliderWidget(this, attributes);
    addWidget(w);
    return w;
  }

  /**
   * Add a checkbox widget
   */
  public CheckBoxWidget addCheckBox(String id, Object... attributePairs) {
    Map<String, Object> attributes = buildAttributes(id, attributePairs);
    CheckBoxWidget w = new CheckBoxWidget(this, attributes);
    addWidget(w);
    return w;
  }

  /**
   * Add a combobox widget
   */
  public ComboBoxWidget addComboBox(String id, Object... attributePairs) {
    Map<String, Object> attributes = buildAttributes(id, attributePairs);
    ComboBoxWidget w = new ComboBoxWidget(this, attributes);
    addWidget(w);
    return w;
  }

  /**
   * Add a button widget
   */
  public ButtonWidget addButton(String id, Object... attributePairs) {
    Map<String, Object> attributes = buildAttributes(id, attributePairs);
    ButtonWidget w = new ButtonWidget(this, attributes);
    addWidget(w);
    return w;
  }

  /**
   * Add a text widget; assigns it a unique id
   * 
   * @param content
   *          the text appearing in the widget
   */
  public TextWidget addStaticText(String content, Object... attributePairs) {
    mPreviousTextIndex++;
    String id = "__text_" + mPreviousTextIndex;
    Map<String, Object> attributes = buildAttributes(id, attributePairs);
    attributes.put(AbstractWidget.OPTION_HAS_LABEL, false);
    attributes.put(AbstractWidget.OPTION_PERSIST_VALUE, false);

    TextWidget w = new TextWidget(this, attributes);
    w.updateUserValue(content);
    addWidget(w);
    return w;
  }

  public TextWidget addEditText(String id, Object... attributePairs) {
    Map<String, Object> attributes = buildAttributes(id, attributePairs);
    TextWidget w = new TextWidget(this, attributes);
    addWidget(w);
    return w;
  }

  public void addLabel(String label) {
    WidgetGroup w = currentWidgetGroup();
    LinearLayout.LayoutParams p = layoutParams(false, 0);
    p.width = MyActivity.getResolutionInfo().inchesToPixelsUI(.8f);
    p.gravity = Gravity.BOTTOM;
    // TODO: is vertical centering being attempted elsewhere, e.g., within
    // buildLabelView?
    w.addView(AbstractWidget.buildLabelView(getContext(), label), p);
  }

  public TextWidget addHeader(String content) {
    return addStaticText(content, TextWidget.OPTION_HEADER, true,
        TextWidget.OPTION_CENTER, true);
  }

  /**
   * Get value of widget (as a string)
   */
  public String getValue(String widgetId) {
    return getWidget(widgetId).getValue();
  }

  /**
   * Get value of widget as an integer
   */
  public int getIntValue(String widgetId) {
    return getWidget(widgetId).getIntValue();
  }

  public void setValue(String widgetId, int intValue) {
    setValue(widgetId, Integer.toString(intValue));
  }

  public void setValue(String widgetId, String strValue) {
    getWidget(widgetId).setValue(strValue);
  }

  public void setEnabled(String widgetId, boolean enabled) {
    getWidget(widgetId).setEnabled(enabled);
  }

  /**
   * Get value of widget as a boolean
   */
  public boolean getBooleanValue(String widgetId) {
    return getWidget(widgetId).getBooleanValue();
  }

  /**
   * Find widget by name
   */
  public <T extends AbstractWidget> T getWidget(String widgetName) {
    T field = (T) findWidget(widgetName);
    if (field == null)
      throw new IllegalArgumentException("no widget found with name "
          + widgetName + "; ids: " + d(mWidgetsMap.keySet(), false));
    return field;
  }

  public AbstractWidget findWidget(String widgetName) {
    mStepper.haveLock();
    AbstractWidget widget = mWidgetsMap.get(widgetName);
    return widget;
  }

  private static Map<String, Object> buildAttributes(String identifier,
      Object attrPairs[]) {
    if (attrPairs.length % 2 != 0)
      throw new IllegalArgumentException();
    Map<String, Object> attributes = new HashMap();
    attributes.put("id", identifier);
    for (int i = 0; i < attrPairs.length; i += 2) {
      attributes.put(attrPairs[i + 0].toString(), attrPairs[i + 1]);
    }
    return attributes;
  }

  /**
   * Get the active WidgetGroup
   */
  private WidgetGroup currentWidgetGroup() {
    boolean algSpecific = (mSecondaryWidgetGroup != null);
    WidgetGroup destination = algSpecific ? mSecondaryWidgetGroup.widgets()
        : mPrimaryWidgetGroup;
    return destination;
  }

  /**
   * Save the current widget container on a stack, and make a user-supplied
   * container active instead
   */
  public void pushView(LinearLayout container) {
    WidgetGroup w = currentWidgetGroup();
    mWidgetContainerStack.add(w.getInnerContainer());
    w.setInnerContainer(container);
  }

  /**
   * Construct a LinearLayout and add it to the widget container
   * 
   * @param vertical
   *          true if layout is to have vertical orientation
   */
  public LinearLayout addView(boolean vertical) {
    LinearLayout newContainer = linearLayout(mContext, vertical);
    addView(newContainer, layoutParams(newContainer, 0));
    return newContainer;
  }

  public void addView(View view, LinearLayout.LayoutParams params) {
    currentWidgetGroup().addView(view, params);
  }

  /**
   * Restore widget container from stack
   */
  public void popView() {
    WidgetGroup w = currentWidgetGroup();
    LinearLayout v = pop(mWidgetContainerStack);
    w.setInnerContainer(v);
  }

  void addWidget(AbstractWidget w) {
    // Add it to the map
    AbstractWidget previousMapping = mWidgetsMap.put(w.getId(), w);
    if (previousMapping != null)
      die("widget id " + w.getId() + " already exists");

    // Add it to the options view, if it's not detached
    if (w.boolAttr(AbstractWidget.OPTION_DETACHED, false))
      return;

    currentWidgetGroup().add(w);

    // If this is being added to the secondary (i.e. algorithm-specific)
    // group, and it's not hidden, flag it as a widget that changes the
    // algorithm total steps
    boolean algSpecific = (mSecondaryWidgetGroup != null);
    if (algSpecific && !w.boolAttr(AbstractWidget.OPTION_HIDDEN, false)) {
      w.setAttribute(AbstractWidget.OPTION_RECALC_ALGORITHM_STEPS, true);
    }
  }

  private ActiveOperationRecord findAlgorithm(String name) {
    for (ActiveOperationRecord rec : mAlgorithms) {
      if (rec.name().equals(name))
        return rec;
    }
    return null;
  }

  /**
   * Compile widget values to string representing a JSON object. The JSON object
   * keys are group names, which are either algorithm names or the special
   * PRIMARY_GROUP_KEY representing the primary widget group.
   * 
   * The JSON object values are themselves JSON objects, whose keys are widget
   * ids, and values are widget values (strings).
   * 
   * @throws JSONException
   * 
   */
  private String saveValues() throws JSONException {
    JSONObject groupValues = new JSONObject();
    groupValues.put(PRIMARY_GROUP_KEY, getWidgetValueMap(mPrimaryWidgetGroup));

    for (ActiveOperationRecord a : mAlgorithms) {
      groupValues.put(a.name(), getWidgetValueMap(a.widgets()));
    }
    return groupValues.toString();
  }

  private JSONObject getWidgetValueMap(WidgetGroup group) {
    Map<String, String> values = new HashMap();
    for (AbstractWidget w : group.widgets()) {
      if (!w.boolAttr(AbstractWidget.OPTION_PERSIST_VALUE, true))
        continue;
      values.put(w.getId(), w.getValue());
    }
    return new JSONObject(values);
  }

  private static final String PRIMARY_GROUP_KEY = "_primarygroup_";

  private void restoreStepperState() {

    try {
      // Provide an empty JSON object as the default
      String script = AppPreferences.getString(
          GeometryStepperActivity.PERSIST_KEY_OPTIONS, "{}");

      JSONObject map = JSONTools.parseMap(script);

      for (String algName : JSONTools.keys(map)) {
        if (algName.equals(PRIMARY_GROUP_KEY)) {
          activateSecondaryWidgetGroup(null);
        } else {
          ActiveOperationRecord rec = findAlgorithm(algName);
          if (rec == null) {
            warning("can't find algorithm '" + algName + "'");
            continue;
          }
          activateSecondaryWidgetGroup(rec);
        }

        JSONObject widgetValues = map.getJSONObject(algName);
        for (String key : JSONTools.keys(widgetValues)) {
          String value = widgetValues.getString(key);
          AbstractWidget w = mWidgetsMap.get(key);
          if (w == null) {
            warning("can't find widget named '" + key + "'");
            continue;
          }
          w.setValue(value);
        }
      }
    } catch (JSONException e) {
      showException(getContext(), e, null);
    }
    mPrepared = true;
    int algNumber = 0;
    algNumber = getIntValue(WIDGET_ID_OPERATION);
    selectAlgorithm(mAlgorithms.get(algNumber));
  }

  /**
   * Save total, target steps to algorithm-specific versions (if there's an
   * active algorithm)
   */
  private void saveStepsInformation() {
    if (mActiveAlgorithm != null && mActiveAlgorithm.isAlgorithm()) {
      setValue(WIDGET_ID_TOTALSTEPS_AUX, readTotalSteps());
      setValue(WIDGET_ID_TARGETSTEP_AUX, readTargetStep());
    }
  }

  private void selectAlgorithm(ActiveOperationRecord ar) {
    if (mActiveAlgorithm == ar)
      return;
    if (mActiveAlgorithm != null) {
      setValue(WIDGET_ID_PREV_OPER, mAlgorithms.indexOf(mActiveAlgorithm));
    }

    QuiescentDelayOperation.cancelExisting(mPendingRecalculationOperation);

    mPrepared = false;

    saveStepsInformation();

    activateSecondaryWidgetGroup(ar);
    if (mActiveAlgorithm != null)
      mContainingView
          .removeView(mActiveAlgorithm.widgets().getOuterContainer());
    mActiveAlgorithm = ar;

    mContainingView.addView(mActiveAlgorithm.widgets().getOuterContainer());

    if (mActiveAlgorithm.isAlgorithm()) {
      // Copy total, target steps from algorithm-specific versions
      setTotalSteps(getIntValue(WIDGET_ID_TOTALSTEPS_AUX));
      setTargetStep(getIntValue(WIDGET_ID_TARGETSTEP_AUX));
    }

    mPrepared = true;

    if (mActiveAlgorithm.isAlgorithm()) {
      // Bound the target step to the total step slider's value. We must
      // do this explicitly here, because the listener that normally does
      // this was disabled while restoring the stepper state
      SliderWidget s = getWidget(WIDGET_ID_TARGETSTEP);
      s.setMaxValue(readTotalSteps());
    }

    mStepper.setAuxViewContent(mActiveAlgorithm.isAlgorithm() ? null : mEditor
        .getAuxControlsView());

    mStepper.refresh();
  }

  private void persistStepperStateAux() {
    if (!mFlushRequired)
      return;

    String newWidgetValuesScript = null;
    synchronized (mStepper.getLock()) {
      mStepper.acquireLock();
      // At present, it only saves widgets that appear in a WidgetGroup.
      // This omits the target step slider, but that's ok, because we have
      // hidden algorithm-specific versions that serve this purpose.
      // But we must now make sure those versions are up to date.
      // Disable recursive flush attempts while updating these (issue
      // #82):
      mPrepared = false;
      saveStepsInformation();
      mPrepared = true;

      try {
        newWidgetValuesScript = saveValues();
      } catch (JSONException e) {
        die(e);
      }
      mStepper.releaseLock();
    }

    AppPreferences.putString(GeometryStepperActivity.PERSIST_KEY_OPTIONS,
        newWidgetValuesScript);

    mFlushRequired = false;
  }

  void persistStepperState(boolean withDelay) {
    mFlushRequired = true;
    if (!withDelay) {
      persistStepperStateAux();
      return;
    }

    // Make a delayed call to persist the values (on the UI thread)
    if (QuiescentDelayOperation.replaceExisting(mPendingFlushOperation)) {
      final float FLUSH_DELAY = 5.0f;
      mPendingFlushOperation = new QuiescentDelayOperation("flush",
          FLUSH_DELAY, new Runnable() {
            public void run() {
              persistStepperStateAux();
            }
          });
    }
  }

  /**
   * Determine if tracing for a particular detail is enabled
   * 
   * @param detailName
   *          name of detail
   * @return value of checkbox with id = detailName
   */
  boolean detailTraceActive(String detailName) {
    AbstractWidget widget = mWidgetsMap.get(detailName);
    if (widget == null) {
      warning("no detail widget found: " + detailName);
      return true;
    }
    return widget.getBooleanValue();
  }

  /**
   * Process a change in a widget's value (or a button click, if it's a button);
   * notify any listeners, and trigger a refresh of the algorithm view
   * 
   * @param widget
   * @param listeners
   *          listeners to notify
   */
  public void processWidgetValue(AbstractWidget widget,
      Collection<Listener> listeners) {

    if (!mPrepared)
      return;

    synchronized (mStepper.getLock()) {
      mStepper.acquireLock();
      for (Listener listener : listeners) {
        listener.valueChanged(widget);
      }

      if (mActiveAlgorithm.isAlgorithm()) {
        // Unless the 'refresh' option exists and is false,
        // trigger a refresh of the algorithm view.
        if (widget.boolAttr(AbstractWidget.OPTION_REFRESH_ALGORITHM, true)) {
          mStepper.refresh();
        }

        // If this is a widget that may change the algorithm total
        // steps, trigger a recalculation after a delay
        if (widget
            .boolAttr(AbstractWidget.OPTION_RECALC_ALGORITHM_STEPS, false)) {
          final float RECALC_DELAY = .5f;
          if (QuiescentDelayOperation
              .replaceExisting(mPendingRecalculationOperation)) {
            mPendingRecalculationOperation = new QuiescentDelayOperation(
                "calc steps", RECALC_DELAY, new Runnable() {
                  public void run() {
                    mStepper.calculateAlgorithmTotalSteps();
                  }
                });
          }
        }
      }
      mStepper.releaseLock();
    }
    persistStepperState(true);
  }

  /**
   * Select the active secondary widget group. Any old auxilliary widget group's
   * widgets are removed from the map, and the new one's are added. This means
   * only one secondary group's widgets are accessible to the options at a time,
   * and allows the same name to be used for different widgets (as long as they
   * are in different secondary groups).
   * 
   * @param group
   *          secondary group, or null
   */
  void activateSecondaryWidgetGroup(ActiveOperationRecord algorithmRecord) {
    if (algorithmRecord == mSecondaryWidgetGroup)
      return;
    if (mSecondaryWidgetGroup != null) {
      for (AbstractWidget w : mSecondaryWidgetGroup.widgets().widgets()) {
        mWidgetsMap.remove(w.getId());
      }
      mSecondaryWidgetGroup = null;
    }
    mSecondaryWidgetGroup = algorithmRecord;
    if (algorithmRecord != null) {
      for (AbstractWidget w : algorithmRecord.widgets().widgets()) {
        mWidgetsMap.put(w.getId(), w);
      }
    }
  }

  void begin(List<Algorithm> algorithms) {
    prepareEditorOperationRecord();
    // Create an ActiveOperationRecord for each algorithm
    for (Algorithm algorithm : algorithms) {
      ActiveOperationRecord algorithmRecord = new ActiveOperationRecord();
      mAlgorithms.add(algorithmRecord);

      algorithmRecord.setDelegate(algorithm);
      algorithmRecord.setWidgetGroup(new WidgetGroup(mContext, true));
      activateSecondaryWidgetGroup(algorithmRecord);
      // Add hidden values to represent total, target steps; these will be
      // copied to/from the main slider as the algorithm becomes
      // active/inactive
      addSlider(WIDGET_ID_TARGETSTEP_AUX, AbstractWidget.OPTION_HIDDEN, true);
      addSlider(WIDGET_ID_TOTALSTEPS_AUX, AbstractWidget.OPTION_HIDDEN, true, //
          // Don't have the total steps trigger a refresh, unlike
          // target step
          AbstractWidget.OPTION_REFRESH_ALGORITHM, false);

      algorithm.prepareOptions(this);
    }
    activateSecondaryWidgetGroup(null);

    addPrimaryWidgets();
    mStepper.addStepperViewListeners();

    restoreStepperState();
    mEditor.refresh();
  }

  /**
   * Read total steps from widget
   */
  int readTotalSteps() {
    return getIntValue(AlgorithmOptions.WIDGET_ID_TOTALSTEPS);
  }

  /**
   * Read target step from widget
   */
  int readTargetStep() {
    return getIntValue(AlgorithmOptions.WIDGET_ID_TARGETSTEP);
  }

  void setTargetStep(int targetStep) {
    setValue(AlgorithmOptions.WIDGET_ID_TARGETSTEP, targetStep);
  }

  void setTotalSteps(int totalSteps) {
    setValue(AlgorithmOptions.WIDGET_ID_TOTALSTEPS, totalSteps);
  }

  public boolean isEditorActive() {
    return mActiveAlgorithm.delegate() == null;
  }

  public Algorithm getActiveAlgorithm() {
    return mActiveAlgorithm.delegate();
  }

  /**
   * This is public only because the widgets are in a different package.
   */
  public Context getContext() {
    return mContext;
  }

  private ConcreteStepper mStepper;
  private LinearLayout mContainingView;
  // Until this flag is true, no listeners are sent messages about widget
  // value changes
  private boolean mPrepared;
  private Context mContext;
  private Map<String, AbstractWidget> mWidgetsMap = new HashMap();
  private WidgetGroup mPrimaryWidgetGroup;
  private List<ActiveOperationRecord> mAlgorithms = new ArrayList();
  private ActiveOperationRecord mActiveAlgorithm;
  private ActiveOperationRecord mSecondaryWidgetGroup;

  private boolean mFlushRequired;
  private QuiescentDelayOperation mPendingFlushOperation;
  private QuiescentDelayOperation mPendingRecalculationOperation;

  // For generating unique text ids
  private int mPreviousTextIndex;
  private Editor mEditor;
  private List<LinearLayout> mWidgetContainerStack = new ArrayList();

}
