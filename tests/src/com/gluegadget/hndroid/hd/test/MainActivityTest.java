package com.gluegadget.hndroid.hd.test;

import com.gluegadget.hndroid.hd.MainActivity;
import com.gluegadget.hndroid.hd.WelcomeFragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.test.ActivityInstrumentationTestCase2;

public class MainActivityTest extends
		ActivityInstrumentationTestCase2<MainActivity> {

	MainActivity activity;
	FragmentManager fragmentManager;
	
	public MainActivityTest() {
		super("com.gluegadget.hndroid.hd", MainActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		activity = getActivity();
		fragmentManager = activity.getFragmentManager();
	}

	private Fragment getDetailsFragment() {
		return fragmentManager.findFragmentById(com.gluegadget.hndroid.hd.R.id.hnDetailsFrame);
	}

	public void testShouldSelectFirstTabAndDisplayWelcomeFragmentOnLaunch() {
		assertEquals(0, activity.getActionBar().getSelectedNavigationIndex());
		assertTrue(getDetailsFragment() instanceof WelcomeFragment);
	}
}
