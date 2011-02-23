package com.gluegadget.hndroid;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.os.Bundle;

public class SubmissionsActivity extends NewsActivity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
		dialog = ProgressDialog.show(SubmissionsActivity.this, "", "Loading. Please wait...", true);
		new Thread(new Runnable(){
			public void run() {
				refreshNews();
				dialog.dismiss();
				handler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			}
		}).start();    
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
	protected Object[] findNewsTitles(TagNode node) throws XPatherException {
		return node.evaluateXPath("//td[@class='title']/a");
	}

	@Override
	protected String findAuthorValue(TagNode author) {
		return "";
	}	
}
