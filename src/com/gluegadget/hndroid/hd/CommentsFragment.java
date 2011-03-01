package com.gluegadget.hndroid.hd;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class CommentsFragment extends Fragment {

	static final private int CONTEXT_REPLY = 1;
	static final private int CONTEXT_UPVOTE = 2;

	ProgressDialog dialog;

	ListView newsListView;

	CommentsAdapter aa;

	ArrayList<Comment> commentsList = new ArrayList<Comment>();
	
	String fnId = "";
	
	Boolean loggedIn = false;

	MenuItem menuItemRefresh;
	MenuItem menuItemComment;
	
	public static CommentsFragment newInstance(String url) {
		Bundle args = new Bundle();
		args.putString("url", url);
		
		CommentsFragment f = new CommentsFragment();
		f.setArguments(args);
		return f;
	}

	public String getCommentsUrl() {
		return getArguments().getString("url");
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.comments_fragment, null);
		newsListView = (ListView) view.findViewById(R.id.comment_list);
		registerForContextMenu(newsListView);
        int layoutID = R.layout.comments_list_item;
        aa = new CommentsAdapter(getActivity(), layoutID , commentsList);
        newsListView.setAdapter(aa);
    	refreshComments();
		return view;
	}

	public boolean canPostComment() {
		return loggedIn && fnId != "";
	}
	
	public void doPostComment() {
		CommentDialog commentDialog = new CommentDialog(getActivity(), "Comment on submission", new OnCommentListener());
		commentDialog.show();
	}

    private class OnCommentListener implements CommentDialog.ReadyListener {
    	@Override
    	public void ready(final String text) {
    		try {
    			dialog = ProgressDialog.show(getActivity(), "", "Trying to comment. Please wait...", true);
    			new Thread(new Runnable(){
    				public void run() {
    					try {
    						DefaultHttpClient httpclient = new DefaultHttpClient();
    						HttpPost httpost = new HttpPost("http://news.ycombinator.com/r");
    						List <NameValuePair> nvps = new ArrayList <NameValuePair>();
    						nvps.add(new BasicNameValuePair("text", text));
    						nvps.add(new BasicNameValuePair("fnid", fnId));
    						httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
    						SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
    			    		String cookie = settings.getString("cookie", "");
    			    		httpost.addHeader("Cookie", "user=" + cookie);
    						httpclient.execute(httpost);
    						httpclient.getConnectionManager().shutdown();
    						dialog.dismiss();
    						notifyCommentAdded();
    					} catch (Exception e) {
    						dialog.dismiss();
    						// TODO: Notify user of failure?
    						e.printStackTrace();
    					}
    				}
    			}).start();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	public void ready(final String text, String url) {}
    }    

    private class OnReplyListener implements CommentDialog.ReadyListener {
    	@Override
    	public void ready(final String text, final String replyUrl) {
    		try {
    			dialog = ProgressDialog.show(getActivity(), "", "Trying to reply. Please wait...", true);
    			new Thread(new Runnable(){
    				public void run() {
    					try {
    						DefaultHttpClient httpclient = new DefaultHttpClient();
    						SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
    			    		String cookie = settings.getString("cookie", "");
    						HttpGet httpget = new HttpGet(replyUrl);
    			    		httpget.addHeader("Cookie", "user=" + cookie);
    			    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    			    		String responseBody = httpclient.execute(httpget, responseHandler);
    			    		HtmlCleaner cleaner = new HtmlCleaner();
    			    		TagNode node = cleaner.clean(responseBody);
    			    		Object[] forms = node.evaluateXPath("//form[@method='post']/input[@name='fnid']");
    			    		if (forms.length == 1) {
    			    			TagNode formNode = (TagNode)forms[0];
    			    			String replyToFnId = formNode.getAttributeByName("value").toString().trim();
    			    			HttpPost httpost = new HttpPost("http://news.ycombinator.com/r");
        						List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        						nvps.add(new BasicNameValuePair("text", text));
        						nvps.add(new BasicNameValuePair("fnid", replyToFnId));
        						httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        			    		httpost.addHeader("Cookie", "user=" + cookie);
        						httpclient.execute(httpost);
        						httpclient.getConnectionManager().shutdown();
    			    		}
    						dialog.dismiss();
    						notifyCommentAdded();
    					} catch (Exception e) {
    						dialog.dismiss();
    						// TODO: Notify user of failure?
    						e.printStackTrace();
    					}
    				}
    			}).start();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	public void ready(final String text) {}
    }

	private void notifyCommentAdded() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				refreshComments();
			}    							
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
	
		menuItemRefresh = menu.add(R.string.menu_refresh_comments);
		menuItemRefresh.setIcon(R.drawable.ic_menu_refresh);
		menuItemRefresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				try {
					refreshComments();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
		});
	
		menuItemComment = menu.add(R.string.menu_comment);
		menuItemComment.setIcon(R.drawable.ic_menu_compose);
		menuItemComment.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menuItemComment.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				doPostComment();
				return true;
			}
		});
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean canPost = canPostComment();
		menuItemComment.setVisible(canPost);
		menuItemComment.setEnabled(canPost);
		super.onPrepareOptionsMenu(menu); 
	}

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	// If we call this, we get all the context menu items from our activity, too.  Ick.
    	//super.onCreateContextMenu(menu, v, menuInfo);
    	
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
    	final Comment newsContexted = (Comment) newsListView.getAdapter().getItem(info.position);
    	
    	menu.setHeaderTitle(newsContexted.getRawText()); 	
    	if (fnId != "" && newsContexted.getReplyToUrl() != "" && loggedIn) {
    		MenuItem originalLink = menu.add(0, CONTEXT_REPLY, 0, R.string.context_reply); 
    		originalLink.setOnMenuItemClickListener(new OnMenuItemClickListener() {		
    			public boolean onMenuItemClick(MenuItem item) {
    				CommentDialog commentDialog = new CommentDialog(
    						getActivity(), "Reply to " + newsContexted.getAuthor(),
    						newsContexted.getReplyToUrl(), new OnReplyListener()
    				);
    				commentDialog.show();

    				return true;
    			}
    		});
    	}
    	
    	if (newsContexted.getUpVoteUrl() != "" && loggedIn) {
    		MenuItem upVote = menu.add(0, CONTEXT_UPVOTE, 0, R.string.context_upvote);
        	upVote.setOnMenuItemClickListener(new OnMenuItemClickListener() {		
        		public boolean onMenuItemClick(MenuItem item) {
        			dialog = ProgressDialog.show(getActivity(), "", "Voting. Please wait...", true);
			    	new Thread(new Runnable(){
			    		public void run() {
			    			SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
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
			    			// The old code used to do this so our upvotes would show up,
			    			// but I find it a little bit disruptive on the larger screen.
			    			//notifyCommentAdded();
			    		}
			    	}).start();
        			return true;
        		}
        	});
    	}
    }

	private void refreshComments() {
		dialog = ProgressDialog.show(getActivity(), "", "Loading. Please wait...", true);
		new Thread(new Runnable(){
			public void run() {
				downloadAndParseComments(getCommentsUrl());
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						dialog.dismiss();
						aa.notifyDataSetChanged();

						// Update our options menu now that we've parsed the page.
						// This is necessary to show the "Comment" icon in the
						// action bar immediately.
						getActivity().invalidateOptionsMenu();
					}
				});
			}
		}).start();
	}

    private void downloadAndParseComments(String uri) {
    	try {
    		commentsList.clear();
    		SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
    		String cookie = settings.getString("cookie", "");
    		DefaultHttpClient httpclient = new DefaultHttpClient();
    		HttpGet httpget = new HttpGet(uri);
    		if (cookie != "")
    			httpget.addHeader("Cookie", "user=" + cookie);
    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    		String responseBody = httpclient.execute(httpget, responseHandler);
    		HtmlCleaner cleaner = new HtmlCleaner();
    		TagNode node = cleaner.clean(responseBody);

    		Object[] loginFnid = node.evaluateXPath("//span[@class='pagetop']/a");
    		TagNode loginNode = (TagNode) loginFnid[5];
    		if (loginNode.getAttributeByName("href").toString().trim().equalsIgnoreCase("submit"))
    			loggedIn = true;
    		Object[] forms = node.evaluateXPath("//form[@method='post']/input[@name='fnid']");
    		if (forms.length == 1) {
    			TagNode formNode = (TagNode)forms[0];
    			fnId = formNode.getAttributeByName("value").toString().trim();	
    		}
    		Object[] comments = node.evaluateXPath("//table[@border='0']/tbody/tr/td/img[@src='http://ycombinator.com/images/s.gif']");

    		if (comments.length > 1) {
    			for (int i = 0; i < comments.length; i++) {
    				TagNode commentNode = (TagNode)comments[i];
    				String depth = commentNode.getAttributeByName("width").toString().trim();
    				Integer depthValue = Integer.parseInt(depth) / 2;
    				TagNode nodeParent = commentNode.getParent().getParent();
    				Object[] comment = nodeParent.evaluateXPath("//span[@class='comment']");
    				Comment commentEntry;
    				if (comment.length > 0) {
    					TagNode commentSpan = (TagNode) comment[0];
    					StringBuffer commentText = commentSpan.getText();
    					if (!commentText.toString().equalsIgnoreCase("[deleted]")) {
    						Object[] score = nodeParent.evaluateXPath("//span[@class='comhead']/span");
    						Object[] author = nodeParent.evaluateXPath("//span[@class='comhead']/a[1]");
    						Object[] replyTo = nodeParent.evaluateXPath("//p/font[@size='1']/u/a");
    						Object[] upVotes = nodeParent.getParent().evaluateXPath("//td[@valign='top']/center/a[1]");

    						TagNode scoreNode = (TagNode) score[0];
    						TagNode authorNode = (TagNode) author[0];

    						String upVoteUrl = "";
    						String replyToValue = "";
    						String scoreValue = scoreNode.getChildren().iterator().next().toString().trim();
    						String authorValue = authorNode.getChildren().iterator().next().toString().trim();
    						if (upVotes.length > 0) {
    							TagNode upVote = (TagNode) upVotes[0];
    							upVoteUrl = upVote.getAttributeByName("href").toString().trim();
    						}
    						if (replyTo.length > 0) {
    							TagNode replyToNode = (TagNode) replyTo[0];
    							replyToValue = replyToNode.getAttributeByName("href").toString().trim();
    						}

                            String commentBody = cleaner.getInnerHtml(commentSpan);
    						commentEntry = new Comment(commentBody, scoreValue, authorValue, depthValue, replyToValue, upVoteUrl);
    					} else {
    						commentEntry = new Comment("[deleted]");
    					}
    					commentsList.add(commentEntry);
    				}
    			}
    		} else {
    			Comment commentEntry = new Comment("No comments.");
    			commentsList.add(commentEntry);
    		}
    	} catch (MalformedURLException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (XPatherException e) {
    		e.printStackTrace();
    	} catch (IllegalStateException e) {
    		// TODO: Can we do something better than this?
    		getActivity().finish();
    	} finally {

    	}
    }
}
