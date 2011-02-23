package com.gluegadget.hndroid;

import android.app.FragmentTransaction;
import android.os.Bundle;

public class CommentsActivity extends HNActivity {
	
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
}
