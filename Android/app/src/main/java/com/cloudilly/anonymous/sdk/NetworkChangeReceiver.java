package com.cloudilly.anonymous.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final ConnectivityManager connectivityManager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo= connectivityManager.getActiveNetworkInfo();
        Intent intentReachability= new Intent("reachabilityChanged");
        String message= networkInfo.isConnectedOrConnecting() ? "connected" : "disconnected";
        intentReachability.putExtra("message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentReachability);
    }
}