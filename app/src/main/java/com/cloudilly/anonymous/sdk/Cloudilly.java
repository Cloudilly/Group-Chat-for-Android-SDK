package com.cloudilly.anonymous.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.koushikdutta.async.AsyncSSLSocket;
import com.koushikdutta.async.AsyncSSLSocketWrapper;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;
import com.koushikdutta.async.callback.DataCallback;
import javax.net.ssl.HostnameVerifier;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class Cloudilly {
    private int CONNECT= 2;
    private int PING= 15;
    private int PONG= 5;
    private Context ctx;
    private AsyncSocket socket;
    private boolean reattempt;
    private int attempts;
    private String app;
    private String saas;
    private int version;
    private String origin;
    private Timer ping;
    private Timer pong;
    private WeakHashMap<Delegate, String> delegates;
    private HashMap<String, JSONObject> tasks;
    private HashMap<String, Object> callbacks;

    public Cloudilly(String app, String access, Context ctx, CallBack cb) {
        LocalBroadcastManager.getInstance(ctx).registerReceiver(appDidBecomeActive, new IntentFilter("appDidBecomeActive"));
        LocalBroadcastManager.getInstance(ctx).registerReceiver(appDidEnterBackground, new IntentFilter("appDidEnterBackground"));
        LocalBroadcastManager.getInstance(ctx).registerReceiver(reachabilityChanged, new IntentFilter("reachabilityChanged"));
        this.ctx= ctx;
        this.reattempt= true;
        this.attempts= 0;
        this.app= app;
        this.saas= "android";
        this.version= 1;
        this.origin= ctx.getPackageName();
        this.delegates= new WeakHashMap<Delegate, String>();
        this.tasks= new HashMap<String , JSONObject>();
        this.callbacks= new HashMap<String , Object>();
        this.callbacks.put("initialized", cb);
        saveToKeyChain(access);
    }

    public void addDelegate(Delegate delegate) {
        this.delegates.put(delegate, "1");
    }

    private void connectToCloudilly() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                AsyncServer.getDefault().connectSocket(new InetSocketAddress("tcp.cloudilly.com", 443), new ConnectCallback() {
                    @Override
                    public void onConnectCompleted(Exception ex, final AsyncSocket sock) {
                        try {
                            if(sock== null) { Log.e("CLOUDILLY", "ERROR: Oops. Something wrong"); return; }
                            socket= sock;
                            handleConnectCompleted(ex, sock, "tcp.cloudilly.com", 443, (CallBack)callbacks.get("initialized"));
                        } catch (NoSuchAlgorithmException | KeyManagementException e) { e.printStackTrace(); }
                    }
                });
            }
        }, 1000* CONNECT* attempts);
    }

    private void disconnectFromCloudilly() {
        reattempt= false;
        if(this.socket== null || !this.socket.isOpen()) { return; }
        this.socket.end();
    }

    private void write(JSONObject dict) {
        StringBuilder data= new StringBuilder();
        data.append(dict.toString());
        data.append("\r\n");
        socket.write(new ByteBufferList(data.toString().getBytes()));
    }

    private void writeAndTask(JSONObject dict, CallBack cb) {
        try {
            Long timestamp= System.currentTimeMillis() / 1000;
            String task= dict.get("type").toString() + "-" + UUID.randomUUID().toString();
            dict.put("task", task);
            JSONObject dictTask= new JSONObject();
            dictTask.put("timestamp", timestamp);
            dictTask.put("data", dict);
            dictTask.put("task", task);
            this.tasks.put(task, dictTask);
            this.callbacks.put(task, cb);
            write(dict);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void connect() {
        try {
            JSONObject keyChain= retrieveFromKeyChain();
            JSONObject dict= new JSONObject();
            dict.put("type", "connect");
            dict.put("app", this.app);
            dict.put("saas", this.saas);
            dict.put("version", this.version);
            dict.put("origin", this.origin);
            dict.put("device", keyChain.get("device"));
            dict.put("access", keyChain.get("access"));
            write(dict);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void connect(String username, String password) {
        try {
            JSONObject keyChain= retrieveFromKeyChain();
            JSONObject dict= new JSONObject();
            dict.put("type", "connect");
            dict.put("app", this.app);
            dict.put("saas", this.saas);
            dict.put("version", this.version);
            dict.put("origin", this.origin);
            dict.put("device", keyChain.get("device"));
            dict.put("access", keyChain.get("access"));
            dict.put("username", username);
            dict.put("password", password);
            write(dict);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void disconnect() {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "disconnect");
            write(dict);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void listen(String group, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "listen");
            dict.put("group", group);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void listen(String group, String password, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "listen");
            dict.put("group", group);
            dict.put("password", password);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void unlisten(String group, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "unlisten");
            dict.put("group", group);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void join(String group, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "join");
            dict.put("group", group);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void join(String group, String password, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "join");
            dict.put("group", group);
            dict.put("password", password);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void unjoin(String group, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "unjoin");
            dict.put("group", group);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void update(JSONObject payload, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "update");
            dict.put("payload", payload);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void post(String group, JSONObject payload, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "post");
            dict.put("group", group);
            dict.put("payload", payload);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void store(String group, JSONObject payload, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "store");
            dict.put("group", group);
            dict.put("payload", payload);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void remove(String post, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "remove");
            dict.put("post", post);
            writeAndTask(dict, callback);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public void create(String group, String password, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "create");
            dict.put("group", group);
            dict.put("password", password);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void login(String username, String password, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "login");
            dict.put("username", username);
            dict.put("password", password);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void logout(CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "logout");
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void register(String token, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "register");
            dict.put("token", token);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void unregister(CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "unregister");
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void link(String group, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "link");
            dict.put("group", group);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void link(String group, String password, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "link");
            dict.put("group", group);
            dict.put("password", password);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void unlink(String group, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "unlink");
            dict.put("group", group);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void notify(String message, String group, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "notify");
            dict.put("message", message);
            dict.put("group", group);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void email(String recipient, String subject, String body, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "email");
            dict.put("recipient", recipient);
            dict.put("subject", subject);
            dict.put("body", body);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void requestPasswordChange(String group, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "requestPasswordChange");
            dict.put("group", group);
            writeAndTask(dict, callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public void changePassword(String group, String password, String token, CallBack callback) {
        try {
            JSONObject dict= new JSONObject();
            dict.put("type", "changePassword");
            dict.put("group", group);
            dict.put("password", password);
            dict.put("token", token);
            writeAndTask(dict,callback);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    private void startPING() {
        if(ping!= null) { return; }
        ping= new Timer();
        ping.schedule(new TimerTask() {
            @Override public void run() { firePING(); }
        }, 1000, PING* 1000);
    }

    private void stopPING() {
        if(ping== null) { return; }
        ping.cancel(); ping.purge(); ping= null;
    }

    private void firePING() {
        StringBuilder ping= new StringBuilder("1\r\n");
        socket.write(new ByteBufferList(ping.toString().getBytes()));
        startPONG();

        HashMap<String, JSONObject> sortedTasks= sortByValues(this.tasks);
        Set set= sortedTasks.entrySet();
        Iterator iterator= set.iterator();
        while(iterator.hasNext()) {
            Map.Entry task= (Map.Entry)iterator.next();
            try {
                JSONObject data= (JSONObject)task.getValue();
                write(data.getJSONObject("data"));
            } catch (JSONException e){e.printStackTrace();}
        }
    }

    private static HashMap<String, JSONObject> sortByValues(HashMap<String, JSONObject> map) {
        List list= new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                try {
                    Double t1= Double.parseDouble(((HashMap.Entry<String, JSONObject>) o1).getValue().get("timestamp").toString());
                    Double t2= Double.parseDouble(((HashMap.Entry<String, JSONObject>) o2).getValue().get("timestamp").toString());
                    if(t1- t2== 0) { return 0; }
                    return t1 - t2 < 0 ? -1 : 1;
                } catch (JSONException e) { e.printStackTrace(); return 0; }
            }
        });
        HashMap sortedHashMap= new LinkedHashMap();
        Iterator iterator= list.iterator();
        while(iterator.hasNext()) { Map.Entry entry= (Map.Entry)iterator.next(); sortedHashMap.put(entry.getKey(), entry.getValue()); }
        return sortedHashMap;
    }

    private void startPONG() {
        if(pong!= null) { pong.cancel(); pong.purge(); pong= null; }
        pong= new Timer();
        Date fireDate= new Date(new Date().getTime()+ (PONG* 1000));
        pong.schedule(new TimerTask() {
            @Override public void run() { firePONG(); }
        }, fireDate);
    }

    private void stopPONG() {
        if(pong== null) { return; }
        pong.cancel(); pong.purge(); pong= null;
    }

    private void firePONG() {
        disconnectFromCloudilly();
    }

    private TrustManager createTrustAllTrustManager() {
        return new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        };
    }

    private void handleConnectCompleted(Exception ex, final AsyncSocket sock, String host, int port, final CallBack cb) throws NoSuchAlgorithmException, KeyManagementException {
        if(ex!= null) { throw new RuntimeException(ex); }
        TrustManager[] trustManagers= new TrustManager[] { createTrustAllTrustManager() };
        SSLContext sslContext= SSLContext.getInstance("TLSv1");
        sslContext.init(null, trustManagers, new SecureRandom());
        SSLEngine sslEngine= sslContext.createSSLEngine();

        HostnameVerifier hostnameVerifier= new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                HostnameVerifier hv= HttpsURLConnection.getDefaultHostnameVerifier();
                return hv.verify("tcp.cloudilly.com", session);
            }
        };

        AsyncSSLSocketWrapper.handshake(sock, host, port, sslEngine, trustManagers, hostnameVerifier, true, new AsyncSSLSocketWrapper.HandshakeCallback() {
            @Override
            public void onHandshakeCompleted(Exception ex, final AsyncSSLSocket sock) {
                if(ex!= null) { throw new RuntimeException(ex); }
                socket= sock;
                cb.onCompleted(null, new JSONObject());
                sock.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        String response= new String(bb.getAllByteArray());
                        String[] responses= response.split("\r\n");
                        for(int i= 0; i< responses.length; i++) {
                            String trimmed= responses[i].trim();
                            if(trimmed.equals("1")) { stopPONG(); continue; }
                            if(trimmed.equals("4000")) { disconnectFromCloudilly(); continue; }

                            try {
                                JSONObject dict= new JSONObject(trimmed);
                                if(!dict.has("type")) { continue; }
                                String type= dict.get("type").toString();

                                if(type.equals("device")) {
                                    Iterator iterator= delegates.keySet().iterator();
                                    while(iterator.hasNext()) { Delegate delegate= (Delegate)iterator.next(); delegate.socketReceivedDevice(dict); }
                                    continue;
                                }

                                if(type.equals("post")) {
                                    Iterator iterator= delegates.keySet().iterator();
                                    while(iterator.hasNext()) { Delegate delegate= (Delegate)iterator.next(); delegate.socketReceivedPost(dict); }
                                    continue;
                                }

                                if(type.equals("connect")) {
                                    String status= dict.get("status").toString();
                                    if(status.equals("success")) { startPING(); }
                                    Iterator iterator= delegates.keySet().iterator();
                                    while(iterator.hasNext()) { Delegate delegate= (Delegate)iterator.next(); delegate.socketConnected(dict); }
                                    continue;
                                }

                                if(type.equals("task")) {
                                    String task= dict.get("task").toString();
                                    final CallBack cb= (CallBack)callbacks.get(task);
                                    cb.onCompleted(null, dict);
                                    callbacks.remove(task);
                                    tasks.remove(task);
                                    continue;
                                }

                            } catch (JSONException e) { e.printStackTrace(); }
                        }
                    }
                });

                sock.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if(ex!= null) { ex.printStackTrace(); }
                        socket= null;
                        attempts++;
                        stopPING();
                        Iterator iterator= delegates.keySet().iterator();
                        while(iterator.hasNext()) { Delegate delegate= (Delegate)iterator.next(); delegate.socketDisconnected(); }
                        if(!reattempt || attempts>= 8) { return; }
                        connectToCloudilly();
                    }
                });
            }
        });
    }

    private void saveToKeyChain(String access) {
        Editor editor= ctx.getSharedPreferences("KeyStoreAndroid", Context.MODE_PRIVATE).edit();
        editor.putString("access", access);
        editor.putString("device", UUID.randomUUID().toString());
        editor.commit();
    }

    private JSONObject retrieveFromKeyChain() {
        SharedPreferences pref= ctx.getSharedPreferences("KeyStoreAndroid", Context.MODE_PRIVATE);
        JSONObject result= new JSONObject();
        try {
            result.put("access", pref.getString("access", ""));
            result.put("device", pref.getString("device", ""));
        } catch(JSONException e) { e.printStackTrace(); }
        return result;
    }

    private BroadcastReceiver appDidBecomeActive= new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { attempts= 0; reattempt= true; connectToCloudilly(); }
    };

    private BroadcastReceiver appDidEnterBackground= new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { disconnectFromCloudilly(); }
    };

    private BroadcastReceiver reachabilityChanged= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message= intent.getStringExtra("message");
            if(message.equals("connected")) { attempts= 0; reattempt= true; connectToCloudilly(); }
            else { Toast.makeText(context, "The internet connection appears to be offline", Toast.LENGTH_LONG).show(); }
        }
    };

    public interface Delegate {
        void socketConnected(JSONObject dict);
        void socketDisconnected();
        void socketReceivedDevice(JSONObject dict);
        void socketReceivedPost(JSONObject dict);
    }

    public interface CallBack {
        void onCompleted(Exception ex, JSONObject dict);
    }
}