package com.cloudilly.anonymous.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class App extends Application {
	protected int running;

	@Override
	public void onCreate() {
		super.onCreate();
		if(android.os.Build.VERSION.SDK_INT< android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) { return; }
		registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			@Override
			public void onActivityResumed(Activity activity) {
				running++; if(running!= 1) { return; }
				Log.e("CLOUDILLY", "@@@@@@ FOREGROUND: " + activity.getLocalClassName());
				Intent intent= new Intent("appDidBecomeActive");
				intent.putExtra("message", "appDidBecomeActive");
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				// [INSERT YOUR CODES FROM HERE >>>


				// >>> TO HERE]
			}

			@Override
			public void onActivityStopped(Activity activity) {
				running--; if(running!= 0) { return; }
				Log.e("CLOUDILLY", "@@@@@@ BACKGROUND: " + activity.getLocalClassName());
				Intent intent= new Intent("appDidEnterBackground");
				intent.putExtra("message", "appDidEnterBackground");
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				// [INSERT YOUR CODES FROM HERE >>>


				// >>> TO HERE]
			}

			@Override public void onActivityCreated(Activity activity, Bundle bundle) { }
			@Override public void onActivityStarted(Activity activity) { }
			@Override public void onActivityPaused(Activity activity) { }
			@Override public void onActivitySaveInstanceState(Activity activity, Bundle bundle) { }
			@Override public void onActivityDestroyed(Activity activity) { }
		});
	}
}