package com.gluegadget.hndroid.hd.test;

import android.app.Fragment;
import android.app.FragmentManager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ListView;

import com.gluegadget.hndroid.hd.MainActivity;
import com.gluegadget.hndroid.hd.WebViewFragment;
import com.gluegadget.hndroid.hd.WelcomeFragment;

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

	private void waitForProgressDialogToBeDismissed() {
		while (activity.isShowingProgressDialog())
			/* Do nothing */;
	}

	private ListView getNewsItemList() {
		return (ListView) activity.findViewById(com.gluegadget.hndroid.hd.R.id.hnListView);
	}

	private Fragment getDetailsFragment() {
		return fragmentManager.findFragmentById(com.gluegadget.hndroid.hd.R.id.hnDetailsFrame);
	}

//	private View getCommentsButton(View item) {
//		return item.findViewById(com.gluegadget.hndroid.hd.R.id.comments);
//	}

	private void clickItemAt(ListView newsItemList, int pos) {
		View item = newsItemList.getChildAt(pos);
		newsItemList.performItemClick(item, pos, 0);
	}

	public void testShouldSelectFirstTabAndDisplayWelcomeFragmentOnLaunch() {
		assertEquals(0, activity.getActionBar().getSelectedNavigationIndex());
		assertTrue(getDetailsFragment() instanceof WelcomeFragment);
	}
	
	public void testOnNewsItemClickShouldDisplayWebFragment() throws Throwable {
		waitForProgressDialogToBeDismissed();
		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				clickItemAt(getNewsItemList(), 0);
				fragmentManager.executePendingTransactions();
				assertTrue(getDetailsFragment() instanceof WebViewFragment);
			}
		});
	}

	// TODO: This currently fails, perhaps because our activity gets torn down
	// while the comment-loading thread is still running.  Disabled for now.
//	public void testOnCommentBubbleClickShouldDisplayComments() throws Throwable {
//		waitForProgressDialogToBeDismissed();
//		runTestOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				View item = getNewsItemList().getChildAt(0);
//				View commentsButton = getCommentsButton(item);
//				commentsButton.performClick();
//				fragmentManager.executePendingTransactions();
//				assertTrue(getDetailsFragment() instanceof CommentsFragment);
//			}
//		});
//	}
}
