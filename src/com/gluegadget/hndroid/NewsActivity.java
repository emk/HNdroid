package com.gluegadget.hndroid;

import java.util.ArrayList;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

abstract class NewsActivity extends Activity {

	protected static final String PREFS_NAME = "user";
	protected static final int NOTIFY_DATASET_CHANGED = 1;
	protected Handler handler;
	protected String newsUrl;
	protected String loginUrl = "";
	protected ProgressDialog dialog;
	protected ListView newsListView;
	protected NewsAdapter aa;
	protected ArrayList<News> news = new ArrayList<News>();
	FrameLayout commentsFrame;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	
		handler = createHandler();
		newsUrl = getDefaultFeedUrl();
	
		newsListView = (ListView)this.findViewById(R.id.hnListView);
		registerForContextMenu(newsListView);
		int layoutID = R.layout.news_list_item;
		aa = new NewsAdapter(this, layoutID , news);
		newsListView.setAdapter(aa);
		newsListView.setOnItemClickListener(clickListener);
		
		commentsFrame = (FrameLayout) findViewById(R.id.hnCommentsFrame);
		if (commentsFrame != null) {
			newsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			// TODO: Automatically reshow what we were last looking at?
	            // This will involve storing more information in our
	            // application.
		}
		
		dialog = ProgressDialog.show(NewsActivity.this, "", "Loading. Please wait...", true);
		new Thread(new Runnable(){
			public void run() {
				refreshNews();
				dialog.dismiss();
				handler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			}
		}).start();
	}

	protected abstract String getDefaultFeedUrl();

	protected Handler createHandler() {
		return new NewsActivityHandler();
	}

	protected class NewsActivityHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case NOTIFY_DATASET_CHANGED:
				aa.notifyDataSetChanged();
				newsListView.setSelection(0);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	OnItemClickListener clickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> newsAV, View view, int pos, long id) {
			final News item = (News) newsAV.getAdapter().getItem(pos);
			if (pos < 30) {
				onNewsItemClicked(pos, item);
			} else {
				dialog = ProgressDialog.show(NewsActivity.this, "", "Loading. Please wait...", true);
		    	new Thread(new Runnable(){
		    		public void run() {
		    			newsUrl = item.getUrl();
		    			refreshNews();
		    			dialog.dismiss();
		    			handler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
		    		}
	
		    	}).start();
			}
		}
	};

	private void onNewsItemClicked(int pos, final News item) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String ListPreference = prefs.getString("PREF_DEFAULT_ACTION", "view-comments");
		if (ListPreference.equalsIgnoreCase("open-in-browser")) {
			Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse((String) item.getUrl()));
			startActivity(viewIntent);
		} else if (ListPreference.equalsIgnoreCase("view-comments")) {
			viewComments(pos, item);
		} else if (ListPreference.equalsIgnoreCase("mobile-adapted-view")) {
			Intent viewIntent = new Intent("android.intent.action.VIEW",
					Uri.parse((String) "http://www.google.com/gwt/x?u=" + item.getUrl()));
			startActivity(viewIntent);
		}
	}

	protected void viewComments(int pos, final News item) {
		String commentsUrl = item.getCommentsUrl();
		String title = item.getTitle();
		if (commentsFrame == null) {
			// We don't have any place to put the comments on this screen,
			// so display them in a new activity.
			Intent intent = new Intent(NewsActivity.this, CommentsActivity.class);
			intent.putExtra("url", commentsUrl);
			intent.putExtra("title", title);
			startActivity(intent);
		} else {
			// We can display the comments inside of this activity.
			newsListView.setItemChecked(pos, true);			
			CommentsFragment fragment = (CommentsFragment)
				getFragmentManager().findFragmentById(R.id.hnCommentsFrame);
			if (fragment == null || fragment.getCommentsUrl() != commentsUrl) {
				fragment = CommentsFragment.newInstance(commentsUrl);
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.hnCommentsFrame, fragment);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
	            ft.commit();
			}
		}
	}

	protected abstract void refreshNews();
}