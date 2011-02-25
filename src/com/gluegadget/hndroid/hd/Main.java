package com.gluegadget.hndroid.hd;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

public class Main extends NewsActivity {
	static final private int LOGIN_FAILED = 2;
	static final private int LOGIN_SUCCESSFULL = 3;
	
	static int DEFAULT_ACTION_PREFERENCES = 0;

	private MenuItem menuItemRefresh;
	private MenuItem menuItemLogout;
	private MenuItem menuItemLogin;
	private MenuItem menuItemPreferences;
	
	@Override
	protected Handler createHandler() {
		return new MainHandler();
	}

	@Override
	protected String getDefaultFeedUrl() {
		return getString(R.string.hnfeed);
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		ActionBar bar = getActionBar();
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		addLinkTab(bar, "Hacker News", "");
		addLinkTab(bar, "Best", "best");
		addLinkTab(bar, "Active", "active");
		// TODO: Note that some links, like /newest, return pages that we can't
		// yet parse.
		//addLinkTab(bar, "New", "newest");
	}

	private void addLinkTab(ActionBar bar, String title, String urlFragment) {
		bar.addTab(bar.newTab().setText(title).setTabListener(new TabListener(urlFragment)));
	}

	private class TabListener implements ActionBar.TabListener {
		String urlFragment;
		
		TabListener(String _urlFragment) {
			urlFragment = _urlFragment;
		}
		
		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// TODO: We should probably try to do something intelligent with
			// the FragmentTransaction here at some point.
			loadNewsForUrlFragment(urlFragment);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

	private final class MainHandler extends NewsActivityHandler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOGIN_FAILED:
				Toast.makeText(Main.this, "Login failed :(", Toast.LENGTH_LONG).show();
				break;
			case LOGIN_SUCCESSFULL:
				Toast.makeText(Main.this, "Successful login :)", Toast.LENGTH_LONG).show();
				refreshNews();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

    private class OnLoginListener implements LoginDialog.ReadyListener {
    	@Override
    	public void ready(final String username, final String password) {
    		try {
    			dialog = ProgressDialog.show(Main.this, "", "Trying to login. Please wait...", true);
    			new Thread(new Runnable(){
    				public void run() {
    					Integer message = 0;
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
    						if (cookies.isEmpty()) {
    							message = LOGIN_FAILED;
    						} else {
    							SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    							SharedPreferences.Editor editor = settings.edit();
    							editor.putString("cookie", cookies.get(0).getValue());
    							editor.commit();
    							message = LOGIN_SUCCESSFULL;
    						}
    						httpclient.getConnectionManager().shutdown();
    						dialog.dismiss();
    						handler.sendEmptyMessage(message);
    					} catch (Exception e) {
    						dialog.dismiss();
    						handler.sendEmptyMessage(message);
    						e.printStackTrace();
    					}
    				}
    			}).start();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);

    	menuItemRefresh = menu.add(R.string.menu_refresh);
    	menuItemRefresh.setIcon(R.drawable.ic_menu_refresh);
    	menuItemRefresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {
    		public boolean onMenuItemClick(MenuItem item) {
    			try {
    				refreshNews();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			return true;
    		}
    	});
    	
    	menuItemLogout = menu.add(R.string.menu_logout);
    	menuItemLogout.setIcon(R.drawable.ic_menu_logout);
    	menuItemLogout.setOnMenuItemClickListener(new OnMenuItemClickListener() {
    		public boolean onMenuItemClick(MenuItem item) {
    			try {
					SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.remove("cookie");
					editor.commit();
					refreshNews();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			return true;
    		}
    	});

    	menuItemLogin = menu.add(R.string.menu_login);
    	menuItemLogin.setIcon(R.drawable.ic_menu_login);
    	menuItemLogin.setOnMenuItemClickListener(new OnMenuItemClickListener() {
    		public boolean onMenuItemClick(MenuItem item) {
    			LoginDialog loginDialog = new LoginDialog(Main.this, "", new OnLoginListener());
    			loginDialog.show();

    			return true;
    		}
    	});
    	
    	menuItemPreferences = menu.add(R.string.menu_preferences);
    	menuItemPreferences.setIcon(R.drawable.ic_menu_preferences);
    	menuItemPreferences.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(Main.this, Preferences.class);
				startActivity(intent);
				
				return true;
			}
		});

    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

    	if (loginUrl.contains("submit")) {
    		menuItemLogin.setVisible(false);
    		menuItemLogin.setEnabled(false);
    		menuItemLogout.setVisible(true);
    		menuItemLogout.setEnabled(true);
    	} else {
    		menuItemLogin.setVisible(true);
    		menuItemLogin.setEnabled(true);
    		menuItemLogout.setVisible(false);
    		menuItemLogout.setEnabled(false);
    	}
    	
    	return super.onPrepareOptionsMenu(menu); 
    }

	private void loadNewsForUrlFragment(final String urlFragment) {
		String hnFeed = getDefaultFeedUrl();
		newsUrl = hnFeed + urlFragment;
		refreshNews();
	}

	/**
	 * Return to the Home activity, clearing any of our other activities
	 * from the current stack.
	 * @param context The current activity.
	 */
	static void returnToMain(Context context) {
		Intent intent = new Intent(context, Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}
}
