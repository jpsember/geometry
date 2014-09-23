package com.js.geometryapp;

import java.util.Map;

import com.js.android.MyActivity;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import static com.js.basic.Tools.*;

public class CheckBoxWidget extends AbstractWidget {

	public static final Factory FACTORY = new AbstractWidget.Factory() {
		@Override
		public String getName() {
			doNothing();
			return "checkbox";
		}

		@Override
		public AbstractWidget constructInstance(Context context, Map attributes) {
			return new CheckBoxWidget(context, attributes);
		}
	};

	public CheckBoxWidget(Context context, Map attributes) {
		super(context, attributes);

		mCheckBox = new CheckBox(context);

		getView().addView(mCheckBox);

		// We generate our own label, since we want it wider and on the right,
		// unlike the usual widget labels
		TextView label;
		{
			label = new TextView(context());
			label.setText(getLabel(false));
			label.setPadding(MyActivity.inchesToPixels(.08f), 0, 0, 0);
			LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			labelParams.gravity = Gravity.BOTTOM;
			label.setLayoutParams(labelParams);
		}
		getView().addView(label);
	}

	/**
	 * Set displayed value; subclasses should perform whatever translation /
	 * parsing is appropriate to convert the internal value to something
	 * displayed in the widget.
	 * 
	 * @param internalValue
	 */
	public void updateUserValue(String internalValue) {
		boolean checked = internalValue.equals("true");
		mCheckBox.setChecked(checked);
	}

	public void setValue(boolean value) {
		setValue(Boolean.toString(value));
	}

	/**
	 * Get displayed value, and transform to 'internal' representation.
	 */
	public String parseUserValue() {
		return mCheckBox.isChecked() ? "true" : "false";
	}

	private CheckBox mCheckBox;
}
