package com.gluegadget.hndroid;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class CommentsActivity extends HNActivity {
	
	static final private int MENU_UPDATE = Menu.FIRST;
	static final private int MENU_COMMENT = Menu.FIRST + 1;
		
	private CommentsFragment comments;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comments);
                
    	final Bundle extras = getIntent().getExtras();
    	final String extrasCommentsUrl = extras.getString("url");
    	getActionBar().setTitle(extras.getString("title"));

    	if (savedInstanceState == null) {    		
    		FragmentTransaction ft =
    			getFragmentManager().beginTransaction();
    		comments = CommentsFragment.newInstance(extrasCommentsUrl);
    		ft.add(R.id.hnComments, comments);
    		ft.commit();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);

    	MenuItem itemRefresh = menu.add(0, MENU_UPDATE, Menu.NONE, R.string.menu_refresh);
    	itemRefresh.setIcon(R.drawable.ic_menu_refresh);
    	itemRefresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {
    		public boolean onMenuItemClick(MenuItem item) {
    			try {
    				comments.doRefreshComments();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			return true;
    		}
    	});

    	MenuItem itemComment = menu.add(0, MENU_COMMENT, Menu.NONE, R.string.menu_comment);
    	itemComment.setIcon(R.drawable.ic_menu_compose);
    	itemComment.setOnMenuItemClickListener(new OnMenuItemClickListener() {
    		@Override
    		public boolean onMenuItemClick(MenuItem item) {
    			comments.doPostComment();
    			return true;
    		}
    	});

    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if (!comments.canPostComment()) {
    		menu.findItem(MENU_COMMENT).setVisible(false);
    		menu.findItem(MENU_COMMENT).setEnabled(false);
    	}
    	
    	return super.onPrepareOptionsMenu(menu); 
    }
}
