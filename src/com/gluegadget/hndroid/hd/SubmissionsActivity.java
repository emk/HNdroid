package com.gluegadget.hndroid.hd;

import android.app.ActionBar;
import android.os.Bundle;

public class SubmissionsActivity extends NewsActivity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	refreshNews();
    }

	@Override
	protected String getDefaultFeedUrl() {
    	final Bundle extras = getIntent().getExtras();
		return "http://news.ycombinator.com/submitted?id=" + extras.getString("user");
	}
	
	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		ActionBar bar = getActionBar();
		Bundle extras = getIntent().getExtras();
		bar.setTitle(extras.getString("title"));
	}

	@Override
	protected int getPageType() {
		return HackerNewsClient.SUBMISSIONS_PAGE;
	}
}
