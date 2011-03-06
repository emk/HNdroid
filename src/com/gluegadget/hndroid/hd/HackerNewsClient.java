package com.gluegadget.hndroid.hd;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.content.Context;
import android.content.SharedPreferences;

public class HackerNewsClient {
	public static final int NEWS_PAGE = 0;
	public static final int SUBMISSIONS_PAGE = 1;
	
	protected static final String PREFS_NAME = "user";
	
	private Context application;

	public HackerNewsClient(Application application) {
		this.application = application;
	}
	
	public String downloadAndParseNews(String newsUrl, int pageType, ArrayList<News> news) {
		String loginUrl = "";
		try {
			news.clear();
			SharedPreferences settings = application.getSharedPreferences(PREFS_NAME, 0);
			String cookie = settings.getString("cookie", "");
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(newsUrl);
			if (cookie != "")
				httpget.addHeader("Cookie", "user=" + cookie);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = httpclient.execute(httpget, responseHandler);
			HtmlCleaner cleaner = new HtmlCleaner();
			TagNode node = cleaner.clean(responseBody);
	
			Object[] newsTitles = findNewsTitles(pageType, node);
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
						authorValue = findAuthorValue(pageType, author);
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
		}
		return loginUrl;
	}
	
	private Object[] findNewsTitles(int pageType, TagNode node) throws XPatherException {
		switch (pageType) {
		case NEWS_PAGE:
			return node.evaluateXPath("//td[@class='title']/a[1]");
		case SUBMISSIONS_PAGE:
			return node.evaluateXPath("//td[@class='title']/a");
		default:
			throw new AssertionError("Unknown pageType");
		}
	}

	private String findAuthorValue(int pageType, TagNode author) {
		switch (pageType) {
		case NEWS_PAGE:
			return author.getChildren().iterator().next().toString().trim();
		case SUBMISSIONS_PAGE:
			return "";
		default:
			throw new AssertionError("Unknown pageType");
		}
	}
}
