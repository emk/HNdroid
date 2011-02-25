package com.gluegadget.hndroid.hd;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

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
		// This makes some mobile-adapted web pages too narrow,
		// and disables the zoom. *sigh*
		//settings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
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
		
		webView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				Toast.makeText(getActivity(),
					R.string.unsupported_content_type,
					Toast.LENGTH_LONG).show();
			}
		});
		
		webView.loadUrl(getUrl());
		return view;
	}
	
	public String getUrl() {
		return getArguments().getString("url");
	}
}
