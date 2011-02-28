package com.gluegadget.hndroid.hd;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CommentsAdapter extends ArrayAdapter<Comment> {
	private LayoutInflater mInflater;
	int resource;
	
	public CommentsAdapter(Context _context, int _resource, List<Comment> _items) {
		super(_context, _resource, _items);
		mInflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		resource = _resource;
	}
	
	static class ViewHolder {
		TextView metadata;
		TextView text;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		Comment item = getItem(position);
		
		if (convertView == null) {
			convertView = mInflater.inflate(resource, parent, false);
			holder = new ViewHolder();
			holder.metadata = (TextView)convertView.findViewById(R.id.metadata);
			holder.text = (TextView)convertView.findViewById(R.id.text);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}

		String metadata = item.getScore();
		if (item.getAuthor() != "")
			metadata += " by " + item.getAuthor();
		holder.metadata.setText(metadata);
		indentView(holder.metadata, item);
		
		holder.text.setText(item.getTitle());
		indentView(holder.text, item);
		return convertView;
	}

	private void indentView(TextView view, Comment item) {
		int t = view.getPaddingTop();
		int r = view.getPaddingRight();
		int b = view.getPaddingBottom();
		view.setPadding(item.getPadding() + 1, t, r, b);
	}
}
