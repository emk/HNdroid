package com.gluegadget.hndroid.hd;

public class Application extends android.app.Application {
	private HackerNewsClient client;
	
	public Application() {
		super();
		client = new HackerNewsClient(this);
	}
	
	public HackerNewsClient getClient() {
		return client;
	}
}
