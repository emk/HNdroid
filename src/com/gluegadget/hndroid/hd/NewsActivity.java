package com.gluegadget.hndroid.hd;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

	protected static final String PREFS_NAME = "user";
	protected static final int NOTIFY_DATASET_CHANGED = 1;
	private static final int CONTEXT_USER_SUBMISSIONS = 2;
	private static final int CONTEXT_COMMENTS = 3;
	private static final int CONTEXT_USER_LINK = 4;
	private static final int CONTEXT_USER_UPVOTE = 5;
	private static final int CONTEXT_GOOGLE_MOBILE = 6;
	protected Handler handler;
	protected String newsUrl;
	protected String loginUrl = "";
	protected ProgressDialog dialog;
	protected ListView newsListView;
	protected NewsAdapter aa;
	protected ArrayList<News> news = new ArrayList<News>();
	boolean showDetailsFrame;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		configureActionBar();
	
		handler = createHandler();
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

	private void detachDetailsFragmentIfPresent() {
		Fragment fragment = getFragmentManager().findFragmentById(R.id.hnDetailsFrame);
		if (fragment != null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.remove(fragment);
			ft.commit();
		}
	}

	protected abstract String getDefaultFeedUrl();

	protected Handler createHandler() {
		return new NewsActivityHandler();
	}

	protected void configureActionBar() {
	}

	protected class NewsActivityHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case NOTIFY_DATASET_CHANGED:
				NewsActivity.this.detachDetailsFragmentIfPresent();
				aa.clearCheckedPosition();
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
    			newsUrl = item.getUrl();
    			refreshNews();
			}
		}
	};

	private void onNewsItemClicked(int pos, final News item) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String ListPreference = prefs.getString("PREF_DEFAULT_ACTION", "open-in-browser");
		if (ListPreference.equalsIgnoreCase("open-in-browser")) {
			viewUrl(pos, item.getUrl());
		} else if (ListPreference.equalsIgnoreCase("view-comments")) {
			viewComments(pos, item);
		} else if (ListPreference.equalsIgnoreCase("mobile-adapted-view")) {
			viewUrl(pos, "http://www.google.com/gwt/x?u=" + item.getUrl());
		}
	}

	private void viewUrl(int pos, String url) {
		if (!showDetailsFrame) {
			resetDetailsFrame();
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
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.hnDetailsFrame, fragment);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft.commit();
			}			
		}
	}

	void viewComments(int pos, final News item) {
		String commentsUrl = item.getCommentsUrl();
		String title = item.getTitle();
		if (!showDetailsFrame) {
			resetDetailsFrame();
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
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.hnDetailsFrame, fragment);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
	            ft.commit();
			}
		}
	}

	private void resetDetailsFrame() {
		detachDetailsFragmentIfPresent();
		aa.clearCheckedPosition();
		aa.notifyDataSetChanged();
	}

	private void updateCheckedNewsItem(int pos) {
		aa.setCheckedPosition(pos);
		aa.notifyDataSetChanged();
	}

	protected void refreshNews() {
		dialog = ProgressDialog.show(this, "", "Loading news. Please wait...", true);
		new Thread(new Runnable(){
			public void run() {
				downloadAndParseNews();
				dialog.dismiss();
				handler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			}
		}).start();
	}
	
	protected void downloadAndParseNews() {
		try {
			news.clear();
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			String cookie = settings.getString("cookie", "");
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(newsUrl);
			if (cookie != "")
				httpget.addHeader("Cookie", "user=" + cookie);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = httpclient.execute(httpget, responseHandler);
			HtmlCleaner cleaner = new HtmlCleaner();
			TagNode node = cleaner.clean(responseBody);
	
			Object[] newsTitles = findNewsTitles(node);
			Object[] subtexts = node.evaluateXPath("//td[@class='subtext']");
			Object[] domains = node.evaluateXPath("//span[@class='comhead']");
			Object[] loginFnid = node.evaluateXPath("//span[@class='pagetop']/a");
			TagNode loginNode = (TagNode) loginFnid[5];
			loginUrl = loginNode.getAttributeByName("href").toString().trim();
	
			if (newsTitles.length > 0) {
				int j = 0;
				int iterateFor = newsTitles.length;
				for (int i = 0; i < iterateFor; i++) {
					String scoreValue = "";
					String authorValue = "";
					String commentValue = "";
					String domainValue = "";
					String commentsUrl = "";
					String upVoteUrl = "";
					TagNode newsTitle = (TagNode) newsTitles[i];
	
					String title = newsTitle.getChildren().iterator().next().toString().trim();
					String href = newsTitle.getAttributeByName("href").toString().trim();
	
					if (i < subtexts.length) {
						TagNode subtext = (TagNode) subtexts[i];
						Object[] scoreSpanNode = subtext.evaluateXPath("/span");
						TagNode score = (TagNode) scoreSpanNode[0];
						
						Object[] scoreAnchorNodes = subtext.evaluateXPath("/a");
						TagNode author = (TagNode) scoreAnchorNodes[0];
						authorValue = findAuthorValue(author);
						if (scoreAnchorNodes.length == 2) {
							TagNode comment = (TagNode) scoreAnchorNodes[1];
							commentValue = comment.getChildren().iterator().next().toString().trim();
						}
	
						TagNode userNode = newsTitle.getParent().getParent();
						Object[] upVotes = userNode.evaluateXPath("//td/center/a[1]");
						if (upVotes.length > 0) {
							TagNode upVote = (TagNode) upVotes[0];
							upVoteUrl = upVote.getAttributeByName("href").toString().trim();
						}
						
						Object[] commentsTag = author.getParent().evaluateXPath("/a");
						if (commentsTag.length == 2)
							commentsUrl = score.getAttributeByName("id").toString().trim();
						
						scoreValue = score.getChildren().iterator().next().toString().trim();
						
						if (href.startsWith("http")) {
							TagNode domain = (TagNode)domains[j];
							domainValue = domain.getChildren().iterator().next().toString().trim();
							j++;
						}
					}
	
					News newsEntry = new News(title, scoreValue, commentValue, authorValue, domainValue, href, commentsUrl, upVoteUrl);
					news.add(newsEntry);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPatherException e) {
			e.printStackTrace();
		} finally {
	
		}
	}

	protected Object[] findNewsTitles(TagNode node) throws XPatherException {
		return node.evaluateXPath("//td[@class='title']/a[1]");
	}

	protected String findAuthorValue(TagNode author) {
		return author.getChildren().iterator().next().toString().trim();
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
						dialog = ProgressDialog.show(NewsActivity.this, "", "Voting. Please wait...", true);
						new Thread(new Runnable(){
							public void run() {
								SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
								String cookie = settings.getString("cookie", "");
								DefaultHttpClient httpclient = new DefaultHttpClient();
								HttpGet httpget = new HttpGet(newsContexted.getUpVoteUrl());
								httpget.addHeader("Cookie", "user=" + cookie);
								ResponseHandler<String> responseHandler = new BasicResponseHandler();
								try {
									httpclient.execute(httpget, responseHandler);
								} catch (ClientProtocolException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								dialog.dismiss();
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