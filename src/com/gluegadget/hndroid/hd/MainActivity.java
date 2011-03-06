package com.gluegadget.hndroid.hd;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

public class MainActivity extends NewsActivity {	
	static int DEFAULT_ACTION_PREFERENCES = 0;

	private MenuItem menuItemRefresh;
	private MenuItem menuItemLogout;
	private MenuItem menuItemLogin;
	private MenuItem menuItemPreferences;
	
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
			loadNewsForUrlFragment(urlFragment, ft);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

	void onLoginFailed() {
		Toast.makeText(MainActivity.this, "Login failed :(", Toast.LENGTH_LONG).show();		
	}
	
	void onLoginSucceeded() {
		Toast.makeText(MainActivity.this, "Successful login :)", Toast.LENGTH_LONG).show();
		refreshNews();		
	}

    private class OnLoginListener implements LoginDialog.ReadyListener {
    	@Override
    	public void ready(final String username, final String password) {
    		try {
    			showProgressDialog("Trying to login. Please wait...");
    			new Thread(new Runnable() {
    				public void run() {
    					final boolean success =
    						getClient().logIn(loginUrl, username, password);
    					hideProgressDialog();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (success)
									onLoginSucceeded();
								else
									onLoginFailed();
							}
						});
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
    				getClient().logOut();
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
    			LoginDialog loginDialog = new LoginDialog(MainActivity.this, "", new OnLoginListener());
    			loginDialog.show();

    			return true;
    		}
    	});
    	
    	menuItemPreferences = menu.add(R.string.menu_preferences);
    	menuItemPreferences.setIcon(R.drawable.ic_menu_preferences);
    	menuItemPreferences.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
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

	private void loadNewsForUrlFragment(final String urlFragment, FragmentTransaction ft) {
		String hnFeed = getDefaultFeedUrl();
		newsUrl = hnFeed + urlFragment;
		refreshNews(ft);
	}

	/**
	 * Return to the Home activity, clearing any of our other activities
	 * from the current stack.
	 * @param context The current activity.
	 */
	static void returnToMain(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}
}
