package com.gluegadget.hndroid.hd;

import java.util.ArrayList;

import android.app.Fragment;
import android.app.ProgressDialog;
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

    private HackerNewsClient getClient() {
		return ((Application) getActivity().getApplication()).getClient();
	}

	private class OnCommentListener implements CommentDialog.ReadyListener {
    	@Override
    	public void ready(final String text) {
    		try {
    			dialog = ProgressDialog.show(getActivity(), "", "Trying to comment. Please wait...", true);
    			new Thread(new Runnable(){
    				public void run() {
    					boolean success = getClient().postComment(text, fnId);
    					dialog.dismiss();
    					if (success)
    						notifyCommentAdded();
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
    					boolean success = getClient().replyToComment(text, replyUrl);
						dialog.dismiss();
						if (success)
							notifyCommentAdded();
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
			    			getClient().upVoteComment(newsContexted);
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
    		HackerNewsClient.CommentPageInfo info =
    			getClient().downloadAndParseComments(uri, commentsList);
    		fnId = info.fnId;
    		loggedIn = info.loggedIn;
    	} catch (IllegalStateException e) {
    		// TODO: Can we do something better than this?
    		// TODO: What is this code doing, anyway?
    		getActivity().finish();
    	}
    }
}
