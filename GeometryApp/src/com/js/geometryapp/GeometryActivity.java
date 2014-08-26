package com.js.geometryapp;

import static com.js.basic.Tools.*;

import java.io.File;
import java.io.IOException;

import com.js.android.MyActivity;
import com.js.basic.Files;
import com.js.geometry.R;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class GeometryActivity extends MyActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			restorePreviousSavedState(savedInstanceState);
		}

		mGLView = new OurGLSurfaceView(this);

		setContentView(mGLView);

		AssetManager m = this.getAssets();

		try {
			String[] assets = m.list("snapshots");
			for (String s : assets) {
				pr(" asset: " + s);
			}
		} catch (IOException e1) {
			die(e1);
		}

		warning("writing sample file");
		try {
			File dir = this.getExternalFilesDir(null);
			if (dir == null)
				die("no external files dir");

			File sampleFile = new File(dir, "___hello___.txt");
			Files.writeTextFile(sampleFile, "This\nis\na\nsample\nfile\n");
		} catch (IOException e) {
			die(e);
		}
	}

	private void restorePreviousSavedState(Bundle savedInstanceState) {
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}

	@Override
	protected void onPause() {
		super.onPause();
		mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mGLView.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			unimp("settings");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public static void setMenuLabel(Menu menu, int menuItemId, String label) {
		MenuItem item = menu.findItem(menuItemId);
		if (item != null) {
			item.setTitle(label);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {

		// There's a bug in the Android API which causes some items to
		// disappear; specifically, the 'exit' item (the last one) is not
		// appearing. Workaround seems to be to delay this code until after the
		// onPrepareCall() completes...

		runOnUiThread(new Runnable() {
			public void run() {
			}
		});
		return super.onPrepareOptionsMenu(menu);
	}

	private OurGLSurfaceView mGLView;
}
