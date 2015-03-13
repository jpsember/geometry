package com.js.geometryapp.widget;

import java.util.Map;

import com.js.geometry.R;
import com.js.geometryapp.AlgorithmOptions;
import com.js.gest.GestureEventFilter;
import com.js.gest.StrokeSet;

import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

public class ButtonWidget extends AbstractWidget {

  public static final GestureEventFilter.Listener GESTURE_LISTENER = new GestureEventFilter.Listener() {
    @Override
    public void strokeSetExtended(StrokeSet strokeSet) {
    }

    @Override
    public void processGesture(String gestureName) {
      pr("...ignoring gesture '" + gestureName + "'");
    }
  };

  public ButtonWidget(AlgorithmOptions options, Map attributes) {
    super(options, attributes);
    attributes.put("hasvalue", false);

    int iconId = intAttr("icon", -1);
    if (iconId < 0) {
      Button b = new Button(options.getContext());
      b.setText(getLabel(false));
      b.setOnClickListener(new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
          notifyListeners();
        }
      });

      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(intAttr(
          OPTION_LAYOUT_WIDTH, LayoutParams.MATCH_PARENT), intAttr(
          OPTION_LAYOUT_HEIGHT, LayoutParams.WRAP_CONTENT));
      getView().addView(b, p);
      mButton = b;
    } else {
      ImageButton b;
      b = new ImageButton(options.getContext());
      b.setImageResource(iconId);
      b.setOnClickListener(new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
          notifyListeners();
        }
      });

      getView().addView(b, getImageButtonLayoutParams());
      mImageButton = b;
    }
  }

  private LayoutParams getImageButtonLayoutParams() {

    // TODO: do this in a once-only initialization somewhere

    // See:
    // http://stackoverflow.com/questions/24213193/android-ignores-layout-weight-parameter-from-styles-xml

    TypedArray a = context().obtainStyledAttributes(R.style.CompactImageButton,
        new int[] { //
        android.R.attr.layout_width, //
            android.R.attr.layout_height, //
            android.R.attr.layout_margin, //
        });
    int width = a.getDimensionPixelSize(0, LayoutParams.WRAP_CONTENT);
    int height = a.getDimensionPixelSize(1, LayoutParams.WRAP_CONTENT);
    int margin = a.getDimensionPixelSize(2, 0);

    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
    p.setMargins(margin, margin, margin, margin);

    a.recycle();
    return p;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (isIcon())
      mImageButton.setEnabled(enabled);
    else
      mButton.setEnabled(enabled);
  }

  private boolean isIcon() {
    if (DEBUG_ONLY_FEATURES) {
    }
    return mImageButton != null;
  }

  private Button mButton;
  private ImageButton mImageButton;
}
