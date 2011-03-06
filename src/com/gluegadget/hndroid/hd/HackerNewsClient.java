package com.gluegadget.hndroid.hd;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.content.SharedPreferences;

/**
 * This is a rather ad hoc class containing all the network client code that
 * used to live in other classes throughout the application.  Our APIs are a
 * little odd, because code was yanked out of other classes and moved here
 * without any real re-design.
 */
public class HackerNewsClient {
	public static class UserInfo {
		String username;
		String karma;
		
		UserInfo(String username, String karma) {
			this.username = username;
			this.karma = karma;
		}
	}

	public static class CommentPageInfo {
		public boolean loggedIn = false;
		public String fnId = "";
	}
	
	public static final int NEWS_PAGE = 0;
	public static final int SUBMISSIONS_PAGE = 1;
	
	private static final String PREFS_NAME = "user";
	
	private Application application;

	public HackerNewsClient(Application application) {
		this.application = application;
	}
	
	private SharedPreferences getSharedPreferences() {
		return application.getSharedPreferences(PREFS_NAME, 0);
	}

	// TODO: Set up a HackerNewsClient for our widget, so that we can make
	// this a regular, non-static method.
	public static UserInfo getUserInfo(String username) {
		try {
			URL url = new URL("http://news.ycombinator.com/user?id=" + username);
			URLConnection connection;
			connection = url.openConnection();

			InputStream in = connection.getInputStream();
			HtmlCleaner cleaner = new HtmlCleaner();
			TagNode node = cleaner.clean(in);
			Object[] userInfo = node.evaluateXPath("//form[@method='post']/table/tbody/tr/td[2]");
			if (userInfo.length > 3) {
				TagNode karmaNode = (TagNode)userInfo[2];
				String karma = karmaNode.getChildren().iterator().next().toString().trim();
				return new UserInfo(username, karma);
			}		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String downloadAndParseNews(String newsUrl, int pageType, ArrayList<News> news) {
		String loginUrl = "";
		try {
			news.clear();
			SharedPreferences settings = getSharedPreferences();
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
	
	public void upVote(News news) {
		SharedPreferences settings = getSharedPreferences();
		String cookie = settings.getString("cookie", "");
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(news.getUpVoteUrl());
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
	}
	
	public boolean logIn(String loginUrl, final String username, final String password) {
		boolean success = false;
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet("http://news.ycombinator.com" + loginUrl);
			HttpResponse response;
			HtmlCleaner cleaner = new HtmlCleaner();
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			TagNode node = cleaner.clean(entity.getContent());
			Object[] loginForm = node.evaluateXPath("//form[@method='post']/input");
			TagNode loginNode = (TagNode) loginForm[0];
			String fnId = loginNode.getAttributeByName("value").toString().trim();			

			HttpPost httpost = new HttpPost("http://news.ycombinator.com/y");
			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("u", username));
			nvps.add(new BasicNameValuePair("p", password));
			nvps.add(new BasicNameValuePair("fnid", fnId));
			httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			response = httpclient.execute(httpost);
			entity = response.getEntity();
			if (entity != null) {
				entity.consumeContent();
			}
			List<Cookie> cookies = httpclient.getCookieStore().getCookies();
			if (!cookies.isEmpty()) {
				SharedPreferences settings = getSharedPreferences();
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("cookie", cookies.get(0).getValue());
				editor.commit();
				success = true;
			}
			httpclient.getConnectionManager().shutdown();
		} catch (Exception e) {
			// TODO: Do something intelligent with errors.
			e.printStackTrace();
		}
		return success;
	}
	
	public void logOut() {
		SharedPreferences settings = getSharedPreferences();
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("cookie");
		editor.commit();
	}
	
	public CommentPageInfo downloadAndParseComments(String uri, ArrayList<Comment> commentsList)
			throws IllegalStateException {
		CommentPageInfo info = new CommentPageInfo();
		try {
			commentsList.clear();
			SharedPreferences settings = getSharedPreferences();
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
				info.loggedIn = true;
			Object[] forms = node.evaluateXPath("//form[@method='post']/input[@name='fnid']");
			if (forms.length == 1) {
				TagNode formNode = (TagNode)forms[0];
				info.fnId = formNode.getAttributeByName("value").toString().trim();	
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
		}
		return info;
	}
	
	public boolean postComment(String text, String fnId) {
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpost = new HttpPost("http://news.ycombinator.com/r");
			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("text", text));
			nvps.add(new BasicNameValuePair("fnid", fnId));
			httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			SharedPreferences settings = getSharedPreferences();
			String cookie = settings.getString("cookie", "");
			httpost.addHeader("Cookie", "user=" + cookie);
			httpclient.execute(httpost);
			httpclient.getConnectionManager().shutdown();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean replyToComment(String text, String replyUrl) {
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			SharedPreferences settings = getSharedPreferences();
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
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void upVoteComment(Comment comment) {
		SharedPreferences settings = getSharedPreferences();
		String cookie = settings.getString("cookie", "");
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(comment.getUpVoteUrl());
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
	}
}
