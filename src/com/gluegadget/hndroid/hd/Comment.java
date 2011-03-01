package com.gluegadget.hndroid.hd;

import android.text.Html;
import android.text.Spanned;

public class Comment {
	private String html;
	private String author;
	private String score;
	private String replyToUrl; 
	private String upVoteUrl;
	private Integer padding;

	public Comment(String _html, String _score, String _author, Integer _padding, String _replyToUrl, String _upVoteUrl) {
		html = _html;
		score = _score;
		author = _author;
		padding = _padding;
		
		if (_replyToUrl.length() > 1)
			replyToUrl = "http://news.ycombinator.com/" + _replyToUrl.replace("&amp", "&");
		else
			replyToUrl = _replyToUrl;
		
		if (_upVoteUrl.length() > 1)
			upVoteUrl = "http://news.ycombinator.com/" + _upVoteUrl.replace("&amp", "&");
		else
			upVoteUrl = _upVoteUrl;
	}

	public Comment(String _html) {
		this(_html, "", "", 0, "", "");
	}
	
	public Integer getPadding() {
		return padding;
	}

	public String getHtml() {
		return html;
	}

	public String getRawText() {
		return getStyledText().toString();
	}
	
	public Spanned getStyledText() {
		return Html.fromHtml(html);
	}
	
	public String getScore() {
		return score;
	}

	public String getAuthor() {
		return author;
	}
	
	public String getReplyToUrl() {
		return replyToUrl;
	}
	
	public String getUpVoteUrl() {
		return upVoteUrl;
	}

	@Override
	public String toString() {
		return author + ": " + html;
	}
}
