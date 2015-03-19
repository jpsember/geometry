package com.js.geometryapp.widget;

import java.util.Map;

import com.js.geometryapp.AlgorithmOptions;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * <pre>
 * 
 * A TextWidget that is non-editable (the default) is represented by a TextView.
 * If the TextWidget is editable, it is represented by an EditText, but one that
 * acts like a button: when clicked, it brings up a dialog that contains a
 * 'standard' EditText, and the (software) keyboard will appear at that point
 * allowing the user to edit the text.  When the user dismisses that dialog, the
 * keyboard will be removed.
 * 
 * </pre>
 */
public class TextWidget extends AbstractWidget {

  /**
   * If true, text is drawn with a larger font
   */
  public static final String OPTION_HEADER = "header";

  /**
   * If true, text is drawn centered horizontally
   */
  public static final String OPTION_CENTER = "center";

  /**
   * Key for (optional) Validator to apply to user-edited content
   */
  public static final String OPTION_VALIDATOR = "validator";

  public TextWidget(AlgorithmOptions options, Map attributes) {
    super(options, attributes);

    if (!boolAttr("editable", false)) {
      mTextView = new TextView(options.getContext());
    } else {
      mEditText = new EditText(options.getContext());
      // We want user to click on this view, like a button, to bring up a
      // dialog to edit its value
      mEditText.setFocusable(false);
      mEditText.setFocusableInTouchMode(false);
      mEditText.setClickable(true);
      mEditText.setOnClickListener(new OnClickListener() {
        public void onClick(View view) {
          displayDialog();
        }
      });
    }

    TextView tv = textView();
    prepareTextView(tv);
    if (boolAttr("header", false)) {
      tv.setTextSize(mTextView.getTextSize() * 1.3f);
    }
    if (boolAttr("center", false)) {
      tv.setGravity(Gravity.CENTER_HORIZONTAL);
    }

    if (boolAttr(OPTION_HAS_LABEL, true))
      getView().addView(buildLabelView(getLabel(true)));

    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(intAttr(
        OPTION_LAYOUT_WIDTH, LayoutParams.MATCH_PARENT), intAttr(
        OPTION_LAYOUT_HEIGHT, LayoutParams.WRAP_CONTENT));
    getView().addView(tv, p);
  }

  @Override
  public void updateUserValue(String internalValue) {
    textView().setText(internalValue);
  }

  @Override
  public String parseUserValue() {
    String content = textView().getText().toString();
    Validator v = (Validator) attributes().get("validator");
    if (v != null) {
      String origContent = content;
      content = v.validate(this, content);
      if (!content.equals(origContent))
        updateUserValue(content);
    }
    return content;
  }

  public void setValidator(Validator v) {
    setAttribute(OPTION_VALIDATOR, v);
  }

  private TextView textView() {
    if (mEditText != null)
      return mEditText;
    return mTextView;
  }

  private void hideKeyboard(EditText editText) {
    InputMethodManager imm = (InputMethodManager) editText.getContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
  }

  private void displayDialog() {
    AlertDialog.Builder alert = new AlertDialog.Builder(context());

    alert.setTitle("Filename");
    String message = strAttr("prompt", "");
    if (!message.isEmpty())
      alert.setMessage(message);

    final EditText editText = new EditText(this.context());
    prepareTextView(editText);

    alert.setView(editText);

    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        hideKeyboard(editText);
        Editable value = editText.getText();
        setValue(value.toString());
      }
    });

    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        hideKeyboard(editText);
      }
    });

    alert.show();

    return;
  }

  private void prepareTextView(TextView editText) {
    editText.setMaxLines(intAttr("maxlines", 1));
    editText.setText(getValue());
  }

  private TextView mTextView;
  private EditText mEditText;
}
