package com.gluegadget.hndroid.hd;

import java.util.ArrayList;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

abstract class NewsActivity extends HNActivity {

	private static final int CONTEXT_USER_SUBMISSIONS = 2;
	private static final int CONTEXT_COMMENTS = 3;
	private static final int CONTEXT_USER_LINK = 4;
	private static final int CONTEXT_USER_UPVOTE = 5;
	private static final int CONTEXT_GOOGLE_MOBILE = 6;
	protected String newsUrl;
	protected String loginUrl = "";
	private ProgressDialog dialog = null;
	protected ListView newsListView;
	protected NewsAdapter aa;
	protected ArrayList<News> news = new ArrayList<News>();
	boolean showDetailsFrame;

	protected HackerNewsClient getClient() {
		return ((Application) this.getApplication()).getClient();
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		configureActionBar();
	
		newsUrl = getDefaultFeedUrl();
	
		newsListView = (ListView)this.findViewById(R.id.hnListView);
		registerForContextMenu(newsListView);
		aa = new NewsAdapter(this, R.layout.news_list_item, news);
		newsListView.setAdapter(aa);
		newsListView.setOnItemClickListener(clickListener);
		updateDetailsFrame();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		updateDetailsFrame();
	}

	private void updateDetailsFrame() {
		showDetailsFrame = getResources().getBoolean(R.bool.show_details);
		int visibility = showDetailsFrame ? View.VISIBLE : View.GONE;
		findViewById(R.id.hnDetailsFrame).setVisibility(visibility);
		if (showDetailsFrame) {
			newsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			// TODO: Automatically reshow what we were last looking at
			// when we're restored from a saved state?
		    // This will involve storing more information in our
		    // application.
		}
	}

	private void detachDetailsFragmentIfPresent(FragmentTransaction ft) {
		Fragment fragment = getFragmentManager().findFragmentById(R.id.hnDetailsFrame);
		if (fragment != null)
			ft.remove(fragment);
		ft.add(R.id.hnDetailsFrame, new WelcomeFragment());
	}

	protected abstract String getDefaultFeedUrl();

	protected void configureActionBar() {
	}

	OnItemClickListener clickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> newsAV, View view, int pos, long id) {
			final News item = (News) newsAV.getAdapter().getItem(pos);
			if (pos < 30) {
				onNewsItemClicked(pos, item);
			} else {
    			newsUrl = item.getUrl();
    			refreshNews();
			}
		}
	};

	private void onNewsItemClicked(int pos, final News item) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String ListPreference = prefs.getString("PREF_DEFAULT_ACTION", "open-in-browser");
		if (ListPreference.equalsIgnoreCase("open-in-browser")) {
			viewUrl(pos, item.getUrl(), ft);
		} else if (ListPreference.equalsIgnoreCase("view-comments")) {
			viewComments(pos, item, ft);
		} else if (ListPreference.equalsIgnoreCase("mobile-adapted-view")) {
			viewUrl(pos, "http://www.google.com/gwt/x?u=" + item.getUrl(), ft);
		}
		
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();		
	}

	private void viewUrl(int pos, String url, FragmentTransaction ft) {
		if (!showDetailsFrame) {
			resetDetailsFrame(ft);
			// We don't have any place to put the web page on this screen,
			// so display it in a new activity.
			Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
			startActivity(viewIntent);
		} else {
			// We can display the URL as an embedded web view.
			updateCheckedNewsItem(pos);
			Fragment fragment = getFragmentManager().findFragmentById(R.id.hnDetailsFrame);
			if (fragment == null || !(fragment instanceof WebViewFragment)
					|| ((WebViewFragment) fragment).getUrl() != url) {
				fragment = WebViewFragment.newInstance(url);
				ft.replace(R.id.hnDetailsFrame, fragment);
			}			
		}
	}

	void viewComments(int pos, final News item) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		viewComments(pos, item, ft);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();		
	}
	
	void viewComments(int pos, final News item, FragmentTransaction ft) {
		String commentsUrl = item.getCommentsUrl();
		String title = item.getTitle();
		if (!showDetailsFrame) {
			resetDetailsFrame(ft);
			// We don't have any place to put the comments on this screen,
			// so display them in a new activity.
			Intent intent = new Intent(NewsActivity.this, CommentsActivity.class);
			intent.putExtra("url", commentsUrl);
			intent.putExtra("title", title);
			startActivity(intent);
		} else {
			updateCheckedNewsItem(pos);
			Fragment fragment = getFragmentManager().findFragmentById(R.id.hnDetailsFrame);
			if (fragment == null || !(fragment instanceof CommentsFragment)
					|| ((CommentsFragment) fragment).getCommentsUrl() != commentsUrl) {
				fragment = CommentsFragment.newInstance(commentsUrl);
				ft.replace(R.id.hnDetailsFrame, fragment);
			}
		}
	}

	private void resetDetailsFrame(FragmentTransaction ft) {
		detachDetailsFragmentIfPresent(ft);
		aa.clearCheckedPosition();
		aa.notifyDataSetChanged();
	}

	private void updateCheckedNewsItem(int pos) {
		aa.setCheckedPosition(pos);
		aa.notifyDataSetChanged();
	}

	// Return true if this activity is currently displaying a progress dialog.
	// This API is included to make testing easier.
	public boolean isShowingProgressDialog() {
		return (dialog != null);
	}
	
	protected void showProgressDialog(String message) {
		dialog = ProgressDialog.show(this, "", message, true);
	}

	protected void hideProgressDialog() {
		dialog.dismiss();
		dialog = null;
	}

	protected void refreshNews() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		refreshNews(ft);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
	}
	
	protected void refreshNews(FragmentTransaction ft) {
		detachDetailsFragmentIfPresent(ft);
		showProgressDialog("Loading news. Please wait...");
		new Thread(new Runnable(){
			public void run() {
				loginUrl = getClient().downloadAndParseNews(newsUrl, getPageType(), news);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						aa.clearCheckedPosition();
						aa.notifyDataSetChanged();
						newsListView.setSelection(0);
						hideProgressDialog();
					}
				});
			}
		}).start();
	}

	protected int getPageType() {
		return HackerNewsClient.NEWS_PAGE;
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		if (info.position < 30) {
			final News newsContexted = (News) newsListView.getAdapter().getItem(info.position);
	
			menu.setHeaderTitle(newsContexted.getTitle());
	
			MenuItem originalLink = menu.add(0, CONTEXT_USER_LINK, 0, newsContexted.getUrl()); 
			originalLink.setOnMenuItemClickListener(new OnMenuItemClickListener() {		
				public boolean onMenuItemClick(MenuItem item) {
					Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse((String) item.getTitle()));
					startActivity(viewIntent);
					return true;
				}
			});
	
			MenuItem googleMobileLink = menu.add(0, CONTEXT_GOOGLE_MOBILE, 0, R.string.context_google_mobile);
			googleMobileLink.setOnMenuItemClickListener(new OnMenuItemClickListener() {		
				public boolean onMenuItemClick(MenuItem item) {
					Intent viewIntent = new Intent("android.intent.action.VIEW",
							Uri.parse((String) "http://www.google.com/gwt/x?u=" + newsContexted.getUrl()));
					startActivity(viewIntent);
					return true;
				}
			});
	
			if (newsContexted.getCommentsUrl() != "") {
				MenuItem comments = menu.add(0, CONTEXT_COMMENTS, 0, R.string.menu_comments); 
				comments.setOnMenuItemClickListener(new OnMenuItemClickListener() {		
					public boolean onMenuItemClick(MenuItem item) {
						viewComments(info.position, newsContexted);
						return true;
					}
				});
			}
	
			maybeAddUserSubmissionsToMenu(menu, newsContexted);
	
			if (loginUrl.contains("submit") && newsContexted.getUpVoteUrl() != "") {
				MenuItem upVote = menu.add(0, CONTEXT_USER_UPVOTE, 0, R.string.context_upvote);
				upVote.setOnMenuItemClickListener(new OnMenuItemClickListener() {		
					public boolean onMenuItemClick(MenuItem item) {
						showProgressDialog("Voting. Please wait...");
						new Thread(new Runnable(){
							public void run() {
								getClient().upVote(newsContexted);
								hideProgressDialog();
								//handler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
							}
						}).start();
						return true;
					}
				});
			}
		}
	}

	private void maybeAddUserSubmissionsToMenu(ContextMenu menu, final News newsContexted) {
		MenuItem userSubmissions = menu.add(0, CONTEXT_USER_SUBMISSIONS, 0, newsContexted.getAuthor() + " submissions");
		userSubmissions.setOnMenuItemClickListener(new OnMenuItemClickListener() {		
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(NewsActivity.this, SubmissionsActivity.class);
				intent.putExtra("user", newsContexted.getAuthor());
				intent.putExtra("title", newsContexted.getAuthor() + " submissions");
				startActivity(intent);
				return true;
			}
		});
	}
}