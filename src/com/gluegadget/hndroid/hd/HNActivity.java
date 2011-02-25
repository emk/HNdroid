package com.gluegadget.hndroid.hd;

import android.app.Activity;
import android.view.MenuItem;

/**
 * A single superclass for all of our activities, except those
 * which inherit from Android classes other than Activity.
 */
public class HNActivity extends Activity {
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			MainActivity.returnToMain(this);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}