package com.gluegadget.hndroid.hd;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WelcomeFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.welcome_fragment, null);
		
		Button emailButton = (Button) view.findViewById(R.id.email_feedback_button);
		emailButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setType("text/plain");
				intent.setData(Uri.parse(getActivity().getString(R.string.developer_email)));
				intent.putExtra(Intent.EXTRA_SUBJECT, getActivity().getString(R.string.feedback_subject));
				getActivity().startActivity(intent);
			}
		});
		
		return view;
	}

}
