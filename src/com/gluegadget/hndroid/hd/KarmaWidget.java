package com.gluegadget.hndroid.hd;

import java.util.LinkedList;
import java.util.Queue;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;

public class KarmaWidget extends AppWidgetProvider {
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (appWidgetIds == null) {
			appWidgetIds = appWidgetManager.getAppWidgetIds(
					new ComponentName(context, KarmaWidget.class));
		}

		UpdateService.requestUpdate(appWidgetIds);
		context.startService(new Intent(context, UpdateService.class));
	}
		
	public static class UpdateService extends Service implements Runnable {
		private static Object sLock = new Object();
		private static Queue<Integer> sAppWidgetIds = new LinkedList<Integer>();
		private static boolean sThreadRunning = false;


		@Override
		public void onStart(Intent intent, int startId) {
			super.onStart(intent, startId);
			
			synchronized (sLock) {
	            if (!sThreadRunning) {
	                sThreadRunning = true;
	                new Thread(this).start();
	            }
	        }
		}

		public static void requestUpdate(int[] appWidgetIds) {
			synchronized (sLock) {
				for (int appWidgetId : appWidgetIds) {
					sAppWidgetIds.add(appWidgetId);
				}
			}
		}

		public RemoteViews buildUpdate(Context context) {
			return null;
		}

		@Override
		public IBinder onBind(Intent intent) {
			// no need to bind
			return null;
		}

		@Override
		public void run() {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
			while (hasMoreUpdates()) {
				int appWidgetId = getNextUpdate();
				updateAppWidget(getApplicationContext(), appWidgetManager, appWidgetId);
			}
		}
		
		private static boolean hasMoreUpdates() {
	        synchronized (sLock) {
	            boolean hasMore = !sAppWidgetIds.isEmpty();
	            if (!hasMore) {
	                sThreadRunning = false;
	            }
	            return hasMore;
	        }
	    }
		
		private static int getNextUpdate() {
	        synchronized (sLock) {
	            if (sAppWidgetIds.peek() == null) {
	                return AppWidgetManager.INVALID_APPWIDGET_ID;
	            } else {
	                return sAppWidgetIds.poll();
	            }
	        }
	    }
	}
	
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.karma_widget);
		
		HackerNewsClient client = new HackerNewsClient(context);
		HackerNewsClient.UserInfo userInfo = client.getUserInfo();
		if (userInfo != null) {
			views.setTextViewText(R.id.username, userInfo.username);
			views.setTextViewText(R.id.karma, userInfo.karma);
		} else {
			views.setTextViewText(R.id.username, "unknown");
			views.setTextViewText(R.id.karma, "0");
		}
		
		Intent intent = new Intent(context, MainActivity.class);
		PendingIntent pending = PendingIntent.getActivity(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.karma_widget, pending);
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}
