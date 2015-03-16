package com.js.android;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * A wrapper for OnTouchListener() that supports chaining.
 * 
 * Subclasses should override onTouch(MotionEvent), instead of
 * onTouch(View,MotionEvent)
 */
public class MyTouchListener implements OnTouchListener {

  public View getView() {
    return mView;
  }

  /**
   * Give listener an opportunity to handle a MotionEvent
   * 
   * @param event
   * @return true if it was handled; false to pass to next handler in chain.
   *         Default implementation returns false
   */
  public boolean onTouch(MotionEvent event) {
    return false;
  }

  @Override
  public boolean onTouch(View view, MotionEvent event) {
    // Eclipse generates warnings if we don't call performClick() at some point
    if (sAlwaysFalse)
      view.performClick();

    MyTouchListener listener = this;
    while (listener != null) {
      if (listener.onTouch(event))
        return true;
      listener = listener.mNextListener;
    }
    return false;
  }

  public void prependTo(MyTouchListener listener) {
    if (listener == null)
      throw new IllegalArgumentException();
    if (listener.mView == null)
      throw new IllegalStateException("next listener has no view");
    mNextListener = listener;
    setViewAux(listener.mView);
  }

  /**
   * Set view for listener chain. Must be called before chain grows beyond a
   * single listener
   */
  public void setView(View view) {
    if (mView != null)
      throw new IllegalStateException("listener chain already has a view");
    setViewAux(view);
  }

  private void setViewAux(View view) {
    mView = view;
    view.setOnTouchListener(this);
  }

  private static boolean sAlwaysFalse = false;
  private MyTouchListener mNextListener;
  private View mView;
}
