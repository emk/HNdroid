package com.gluegadget.hndroid;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.os.Bundle;
import android.widget.TextView;

public class Submissions extends NewsActivity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	final Bundle extras = getIntent().getExtras();
    	TextView hnTopDesc = (TextView)this.findViewById(R.id.hnTopDesc);
    	hnTopDesc.setText(extras.getString("title"));
    }

	@Override
	protected String getDefaultFeedUrl() {
    	final Bundle extras = getIntent().getExtras();
		return "http://news.ycombinator.com/submitted?id=" + extras.getString("user");
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
