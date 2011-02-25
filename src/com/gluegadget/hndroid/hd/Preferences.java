package com.gluegadget.hndroid.hd;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class Preferences extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// This code is identical to HNActivity.  Normally, we'd
		// just inherit from it, but we need to inherit from
		// PreferenceActivity instead.
		if (item.getItemId() == android.R.id.home) {
			MainActivity.returnToMain(this);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}