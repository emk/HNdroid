package com.gluegadget.hndroid;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class NewsAdapter extends ArrayAdapter<News> {
	private LayoutInflater mInflater;
	
	NewsActivity context;
	
	int resource;
	
	public NewsAdapter(NewsActivity _context, int _resource, List<News> _items) {
		super(_context, _resource, _items);
		mInflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		resource = _resource;
		context = _context;
	}
	
	static class ViewHolder {
		TextView title;
		TextView score;
		TextView comment;
		TextView author;
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		final News item = getItem(position);
		
		if (convertView == null) {
			convertView = mInflater.inflate(resource, parent, false);
			holder = new ViewHolder();
			holder.title = (TextView)convertView.findViewById(R.id.title);
			holder.score = (TextView)convertView.findViewById(R.id.score);
			holder.comment = (TextView)convertView.findViewById(R.id.comments);
			holder.author = (TextView)convertView.findViewById(R.id.author);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}
		
		holder.title.setText(item.getTitle());
		holder.score.setText(item.getScore());
		holder.comment.setText(item.getComment());
		String[] commentButtonTag = { item.getTitle(), item.getCommentsUrl() };
		holder.comment.setTag(commentButtonTag);
		holder.comment.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.viewComments(position, item);
			}
		});

		if (item.getAuthor() == "")
			holder.author.setText(item.getAuthor());
		else
			if (item.getDomain() == "")
				holder.author.setText("by " + item.getAuthor());
			else
				holder.author.setText("by " + item.getAuthor() + " from " + item.getDomain());
		

		return convertView;
	}
}