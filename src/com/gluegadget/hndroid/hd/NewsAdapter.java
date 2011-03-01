package com.gluegadget.hndroid.hd;

import java.util.List;

import android.content.Context;
import android.util.TypedValue;
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
	
	int defaultBackgroundColor;
	
	int checkedPosition = -1;
	
	public NewsAdapter(NewsActivity _context, int _resource, List<News> _items) {
		super(_context, _resource, _items);
		mInflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		resource = _resource;
		context = _context;
		defaultBackgroundColor = getDefaultBackgroundColor();
	}
	
	public void setCheckedPosition(int _checkedPosition) {
		checkedPosition = _checkedPosition;
	}
	
	public void clearCheckedPosition() {
		setCheckedPosition(-1);
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
			    
	    if (position == checkedPosition) {
	    	int highlight = context.getResources().getColor(R.color.news_item_highlight);
			convertView.setBackgroundColor(highlight);
	    } else {
			convertView.setBackgroundColor(defaultBackgroundColor);
	    }

		return convertView;
	}

	private int getDefaultBackgroundColor() {
		// http://stackoverflow.com/questions/2826739/how-to-extract-color-values-rgb-from-an-android-theme
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
		int backgroundColor = context.getResources().getColor(tv.resourceId);
		return backgroundColor;
	}
}
