package com.gluegadget.hndroid.hd;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * Honeycomb also provides a WebViewFragment, but nobody has bothered
 * to document it yet, and I haven't been to get it working.  So let's
 * reinvent the wheel, and try to keep it simple.
 */
public class WebViewFragment extends Fragment {
	ProgressBar progressBar;
	WebView webView;
	
	public static WebViewFragment newInstance(String url) {
		Bundle args = new Bundle();
		args.putString("url", url);
		
		WebViewFragment fragment = new WebViewFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	public String getUrl() {
		return getArguments().getString("url");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.web_view_fragment, null);
		
		progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		progressBar.setMax(100);
		
		webView = (WebView) view.findViewById(R.id.web_view);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setBuiltInZoomControls(true);
		settings.setPluginState(WebSettings.PluginState.ON);
		// This makes some mobile-adapted web pages too narrow,
		// and disables the zoom. *sigh*
		//settings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
		settings.setUseWideViewPort(true);
		webView.setInitialScale(0);

		webView.setWebChromeClient(new WebChromeClient () {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				progressBar.setIndeterminate(false);
				progressBar.setProgress(newProgress);
			}
		});
		
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				progressBar.setVisibility(View.GONE);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// Always open links externally.  I thought this was the
				// default, but apparently not.
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				getActivity().startActivity(intent);
				return true;
			}
		});

		webView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				getActivity().startActivity(intent);
			}
		});
		
		webView.loadUrl(getUrl());
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		MenuItem menuItemShare = menu.add(R.string.menu_share);
		menuItemShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menuItemShare.setIcon(android.R.drawable.ic_menu_share);
		menuItemShare.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TITLE, webView.getTitle());
				intent.putExtra(Intent.EXTRA_SUBJECT, webView.getTitle());
				intent.putExtra(Intent.EXTRA_TEXT, getUrl());
				getActivity().startActivity(intent);
				return true;
			}
		});
	}
}
