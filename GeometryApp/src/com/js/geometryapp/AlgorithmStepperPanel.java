package com.js.geometryapp;

import com.js.geometry.R;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.ButtonWidget;
import com.js.geometryapp.widget.SliderWidget;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import static com.js.android.UITools.*;
import static com.js.basic.Tools.*;

/**
 * View that contains the step controls (slider and page / step buttons)
 */
class AlgorithmStepperPanel {

  AlgorithmStepperPanel(AlgorithmOptions options) {
    doNothing();
    mOptions = options;
  }

  private void addButton(LinearLayout parent, String label, int iconId) {
    ButtonWidget w = mOptions.addButton(label, "icon", iconId,
        AbstractWidget.OPTION_DETACHED, true,
        AbstractWidget.OPTION_REFRESH_ALGORITHM, false);
    parent.addView(w.getView(), layoutParams(parent, 0));
  }

  View view() {
    if (mView != null)
      return mView;

    Context context = mOptions.getContext();
    LinearLayout layout = linearLayout(context, false);

    final int defaultTotalSteps = 500;

    final SliderWidget targetStepSlider = mOptions.addSlider(
        AlgorithmOptions.WIDGET_ID_TARGETSTEP, //
        AbstractWidget.OPTION_DETACHED, true, //
        AbstractWidget.OPTION_HAS_LABEL, false //
        );
    layout.addView(targetStepSlider.getView(), layoutParams(layout, 1));

    // Add another detached widget to store the total steps
    AbstractWidget totalStepsSlider = mOptions.addSlider(
        AlgorithmOptions.WIDGET_ID_TOTALSTEPS,//
        "value", defaultTotalSteps,//
        AbstractWidget.OPTION_DETACHED, true);

    // Listen for changes to total steps, to change maximum value of target
    // step
    totalStepsSlider.addListener(new AbstractWidget.Listener() {
      @Override
      public void valueChanged(AbstractWidget widget) {
        targetStepSlider.setMaxValue(widget.getIntValue());
      }
    });

    LinearLayout v1 = linearLayout(context, true);
    layout.addView(v1, layoutParams(layout, 0));
    {
      LinearLayout v2 = linearLayout(context, false);
      addButton(v2, ConcreteStepper.WIDGET_ID_JUMP_BWD, R.raw.jumpbwdicon);
      addButton(v2, ConcreteStepper.WIDGET_ID_JUMP_FWD, R.raw.jumpfwdicon);
      v1.addView(v2);
    }
    {
      LinearLayout v2 = linearLayout(context, false);
      addButton(v2, ConcreteStepper.WIDGET_ID_STEP_BWD, R.raw.stepbwdicon);
      addButton(v2, ConcreteStepper.WIDGET_ID_STEP_FWD, R.raw.stepfwdicon);
      v1.addView(v2);
    }

    mView = layout;
    return mView;
  }

  private View mView;
  private AlgorithmOptions mOptions;
}
