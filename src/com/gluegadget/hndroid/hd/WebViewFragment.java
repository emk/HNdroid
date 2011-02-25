package com.gluegadget.hndroid.hd;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Honeycomb also provides a WebViewFragment, but nobody has bothered
 * to document it yet, and I haven't been to get it working.  So let's
 * reinvent the wheel, and try to keep it simple.
 */
class WebViewFragment extends Fragment {
	WebView view;
	
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
		view = new WebView(getActivity());
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setBuiltInZoomControls(true);
		view.loadUrl(getUrl());
		return view;
	}
	
	public String getUrl() {
		return getArguments().getString("url");
	}
}
