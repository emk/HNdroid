package com.gluegadget.hndroid.hd;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

/**
 * Honeycomb also provides a WebViewFragment, but nobody has bothered
 * to document it yet, and I haven't been to get it working.  So let's
 * reinvent the wheel, and try to keep it simple.
 */
class WebViewFragment extends Fragment {
	ProgressBar progressBar;
	WebView webView;
	
	public static WebViewFragment newInstance(String url) {
		Bundle args = new Bundle();
		args.putString("url", url);
		
		WebViewFragment fragment = new WebViewFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.web_view_fragment, null);
		
		progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		progressBar.setMax(100);
		progressBar.setIndeterminate(true);
		
		webView = (WebView) view.findViewById(R.id.web_view);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setBuiltInZoomControls(true);
		settings.setPluginState(WebSettings.PluginState.ON);
		settings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
		settings.setUseWideViewPort(true);
		webView.setInitialScale(0);
		
		webView.setWebChromeClient(new WebChromeClient () {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				if (newProgress < 100) {
					progressBar.setIndeterminate(false);
					progressBar.setProgress(newProgress);
				} else {
					progressBar.setVisibility(View.GONE);
					webView.setVisibility(View.VISIBLE);
				}
			}
		});
		
		webView.loadUrl(getUrl());
		return view;
	}
	
	public String getUrl() {
		return getArguments().getString("url");
	}
}
