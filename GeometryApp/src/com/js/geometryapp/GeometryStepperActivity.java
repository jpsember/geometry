package com.js.geometryapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.js.android.AppPreferences;
import com.js.basic.Files;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.R;
import com.js.geometryapp.editor.Editor;
import com.js.geometryapp.editor.EditorTools;
import com.js.gest.GesturePanel;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import static com.js.basic.Tools.*;
import static com.js.android.Tools.*;
import static com.js.android.UITools.*;

public abstract class GeometryStepperActivity extends GeometryActivity {

  public static final String PERSIST_KEY_OPTIONS = "_widget_values";
  public static final String PERSIST_KEY_EDITOR = "_editor";
  private static final int REQUEST_SHARE_GEOM_FILE = 1000;

  public GeometryStepperActivity() {
    /**
     * <pre>
     * 
     * Activity initialization has these stages:
     * 
     * 1) construct the components (the nodes of an object graph)
     * 2) establish dependencies between components (the edges of the graph)
     * 
     * </pre>
     */

    // Stage 1: construct the various components
    mEditor = new Editor();
    mStepper = new ConcreteStepper();
    mOptions = new AlgorithmOptions(this);
    mRenderer = new AlgorithmRenderer(this);

    // Stage 2: establish dependencies
    mEditor.setDependencies(this, mStepper, mOptions, mRenderer);
    mStepper.setDependencies(mOptions, mEditor);
    mOptions.setDependencies(mEditor, mStepper);
    mRenderer.setDependencies(mEditor, mStepper);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Set the theme to appply to the entire application.
    // AppTheme is defined in res/values/styles.xml:
    setTheme(R.style.AppTheme);
    GesturePanel gesturePanel = new GesturePanel(this);
    mEditor.setGesturePanel(gesturePanel);
    mOptions.setGesturePanel(gesturePanel);
    super.onCreate(savedInstanceState);

    addAlgorithms(mStepper);
    mStepper.begin();
    restoreEditorPreferences();
    processIntent();
    mStepper.refresh();
  }

  private void restoreEditorPreferences() {
    String script = AppPreferences.getString(
        GeometryStepperActivity.PERSIST_KEY_EDITOR, null);
    if (script != null)
      mEditor.restoreFromJSON(script);
  }

  public abstract void addAlgorithms(AlgorithmStepper s);

  @Override
  protected void onResume() {
    super.onResume();
    if (mGLView != null)
      mGLView.onResume();
  }

  @Override
  protected void onPause() {
    mOptions.persistStepperState(false);
    mEditor.persistEditorState(false);
    super.onPause();
    if (mGLView != null)
      mGLView.onPause();
  }

  @Override
  protected View buildContentView() {
    GLSurfaceView surfaceView = new GLSurfaceView(this);
    surfaceView.setEGLContextClientVersion(2);

    mGLView = surfaceView;
    surfaceView.setRenderer(mRenderer);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    mStepper.setGLSurfaceView(surfaceView);

    // Build a view that will contain the GLSurfaceView and a stepper
    // control panel
    LinearLayout mainView = linearLayout(this, true);
    {
      // Wrap the GLSurfaceView within another container, so we can
      // overlay it with an editing toolbar
      mEditor.prepare(surfaceView);

      View editorView = mEditor.getView();
      // Place editor view within a container with a black background
      // to emphasize boundary between the editor and the neighbors
      {
        LinearLayout borderView = linearLayout(this, true);
        borderView.setPadding(2, 2, 2, 2);
        borderView.setBackgroundColor(Color.rgb(128, 128, 128));
        borderView.addView(editorView, layoutParams(borderView, 1));
        editorView = borderView;
      }
      LinearLayout.LayoutParams p = layoutParams(mainView, 1);
      mainView.addView(editorView, p);
    }

    // Add the stepper control panel to this container
    View stepperControllerView = mStepper.buildControllerView();
    LinearLayout.LayoutParams p = layoutParams(mainView, 0);
    mainView.addView(stepperControllerView, p);

    // Make the container the main view of a TwinViewContainer
    TwinViewContainer twinViews = new TwinViewContainer(this, mainView);
    mOptions.prepareViews(twinViews.getAuxilliaryView());
    return twinViews.getContainer();
  }

  private static String dumpIntent(Intent intent) {
    if (!DEBUG_ONLY_FEATURES)
      return intent.toString();
    StringBuilder sb = new StringBuilder("Intent(");
    sb.append("\n type:       " + d(intent.getType()));
    sb.append("\n action:     " + d(intent.getAction()));
    sb.append("\n categories: " + d(intent.getCategories()));
    sb.append("\n data:       " + intent.getDataString());
    sb.append("\n extras:     " + d(intent.getExtras()));
    sb.append("\n)");
    return sb.toString();
  }

  /**
   * Process intent; read editor objects from its contents if possible
   */
  private void processIntent() {
    final boolean db = false && DEBUG_ONLY_FEATURES;
    Intent intent = getIntent();
    if (intent == null)
      return;
    if (db)
      pr("\n\nprocess " + dumpIntent(intent) + "\n\n");

    /**
     * <pre>
     * 
     * My Files:
     * ----------
     * process Intent( 
     *  type:        
     *  action:     android.intent.action.VIEW 
     *  categories: <null> 
     *  data:       file:///storage/emulated/0/Download/b1_00.geom 
     *  extras:       "Bundle[mParcelledData.dataSize=616]" 
     * ) 
     *            
     * Dropbox:
     * -----------
     * process Intent( 
     *  type:       application/octet-stream 
     *  action:     android.intent.action.VIEW 
     *  categories: <null> 
     *  data:       file:///storage/emulated/0/Android/data/com.dropbox.android/files/scratch/a1.geom 
     *  extras:       "Bundle[mParcelledData.dataSize=164]" 
     * )
     * 
     * GMail:
     * -----------
     * process Intent( 
     *  type:       application/octet-stream 
     *  action:     android.intent.action.VIEW 
     *  categories: <null> 
     *  data:       content://gmail-ls/jpsember@gmail.com/messages/960/attachments/0.1/BEST/false 
     *  extras:       <null> 
     * )
     * 
     * 
     * Drive:
     * -----------
     * process Intent( 
     *  type:       application/octet-stream 
     *  action:     android.intent.action.VIEW 
     *  categories: <null> 
     *  data:       file:///data/data/com.google.android.apps.docs/files/fileinternal/74eda9f25aa2f91a461546b60100b39e/b1_00.geom 
     *  extras:       "Bundle[mParcelledData.dataSize=412]" 
     * )
     * 
     * </pre>
     */
    String jsonContent = null;
    if (Intent.ACTION_VIEW.equals(intent.getAction())) {
      try {
        Uri u = intent.getData();
        String scheme = u.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
          // handle as content uri
          InputStream stream = getContentResolver().openInputStream(u);
          jsonContent = Files.readString(stream);
        } else {
          File f = new File(u.getPath());
          jsonContent = Files.readString(f);
        }
      } catch (IOException e) {
        toast(this, "Problem reading file");
        pr(e);
      }
    }
    if (jsonContent == null)
      return;
    mEditor.restoreFromJSON(jsonContent);
  }

  /**
   * Share a data file via email
   * 
   * @param name
   *          name given to file by user; will be incorporated into filename; if
   *          empty, uses "unknown"
   * @param attachment
   *          data file to include as attachment
   */
  public void doShare(String name, byte[] attachment) {
    name = EditorTools.sanitizeFilename(name);
    if (name.isEmpty())
      name = "unknown";
    String filename = name + ".geom";

    String recipient = "";
    String subject = "Geometry Framework data file: " + filename;
    String message = "";

    final Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("message/rfc822");
    intent.putExtra(Intent.EXTRA_EMAIL, new String[] { recipient });
    intent.putExtra(Intent.EXTRA_SUBJECT, subject);
    intent.putExtra(Intent.EXTRA_TEXT, message);

    // create attachment

    File file = new File(getExternalCacheDir(), filename);
    Throwable problem = null;
    try {
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(attachment);
      fos.close();
    } catch (IOException e) {
      problem = e;
    }

    if (problem != null || !file.exists() || !file.canRead()) {
      String toastMessage = "Problem creating attachment";
      if (problem != null)
        toastMessage += ": " + problem;
      Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
      return;
    }

    Uri uri = Uri.parse("file://" + file.getAbsolutePath());
    intent.putExtra(Intent.EXTRA_STREAM, uri);

    startActivityForResult(
        Intent.createChooser(intent, "Email Geometry file using..."),
        REQUEST_SHARE_GEOM_FILE);
  }

  private ConcreteStepper mStepper;
  private AlgorithmOptions mOptions;
  private AlgorithmRenderer mRenderer;
  private Editor mEditor;
  private GLSurfaceView mGLView;
}
