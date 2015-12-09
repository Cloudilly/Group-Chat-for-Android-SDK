package com.cloudilly.anonymous;

import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.cloudilly.anonymous.sdk.Cloudilly;

public class ChatActivity extends AppCompatActivity implements Cloudilly.Delegate {
	private Cloudilly cloudilly;
	private ListView chatsListView;
	private EditText inputEditText;
	private Button sendButton;
	private ArrayAdapter<String> messageListAdapter;
	ArrayList<String> messageList= new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		String app= "com.cloudilly.anonymous";
		String access= "0092b106-b3df-4cf6-bec6-4df9ce34b0d8";
		cloudilly= new Cloudilly(app, access, getApplicationContext(), new Cloudilly.CallBack() {
			@Override
			public void onCompleted(Exception ex, JSONObject dict) {
				updateView("CONNECTING...");
				cloudilly.connect();
			}
		});
		cloudilly.addDelegate(this);

		chatsListView= (ListView)findViewById(R.id.chatsListView);
		inputEditText= (EditText)findViewById(R.id.inputEditText);
		sendButton= (Button)findViewById(R.id.sendButton);

		inputEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled= false;
				if(actionId== EditorInfo.IME_ACTION_SEND) { handled= true; fireSend(); }
				return handled;
			}
		});

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { fireSend(); }
		});

		messageListAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, messageList);
		chatsListView.setAdapter(messageListAdapter);
	}

	protected void updateView(final String msg) {
		if(msg== null || messageListAdapter== null) { return; }
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				messageList.add(msg);
				messageListAdapter.notifyDataSetChanged();
			}
		});
	}

	private void fireSend() {
		final String msg= inputEditText.getText().toString();
		if(msg.length()== 0) { return; }
		InputMethodManager imm= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
		postMessage(inputEditText.getText().toString());
		inputEditText.setText("");
	}

	private void postMessage(String msg) {
		try {
			JSONObject payload= new JSONObject();
			payload.put("msg", msg);
			cloudilly.post("public", payload, new Cloudilly.CallBack() {
				@Override
				public void onCompleted(Exception ex, JSONObject dict) {
					Log.e("CLOUDILLY", "@@@@@@ POST: " + dict.toString());
					try {
						if (dict.get("status").toString().equalsIgnoreCase("fail")) {
							Log.e("CLOUDILLY", "ERROR: Oops. Something wrong");
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
		} catch(JSONException e) { e.printStackTrace(); }
	}

	@Override
	public void socketConnected(JSONObject dict) {
		Log.e("CLOUDILLY", "@@@@@@ CONNECTED: " + dict.toString());
		try {
			if(dict.get("status").toString().equalsIgnoreCase("fail")) { Log.e("CLOUDILLY", "ERROR: Oops. Something wrong"); return; }
			updateView("CONNECTED AS " + dict.get("device").toString().toUpperCase());

			cloudilly.join("public", new Cloudilly.CallBack() {
				@Override
				public void onCompleted(Exception ex, JSONObject dict) {
					Log.e("CLOUDILLY", "@@@@@@ JOIN: " + dict.toString());
					try {
						if(dict.get("status").toString().equalsIgnoreCase("fail")) { Log.e("CLOUDILLY", "ERROR: Oops. Something wrong"); return; }
						updateView("DEVICES PRESENT IN PUBLIC: " + dict.getInt("total_devices"));
					} catch (JSONException e) { e.printStackTrace(); }
				}
			});
		} catch (JSONException e) { e.printStackTrace(); }
	}

	@Override
	public void socketDisconnected() {
		Log.e("CLOUDILLY", "@@@@@@ DISCONNECTED");
		updateView("DISCONNECTED");
	}

	@Override
	public void socketReceivedDevice(JSONObject dict) {
		Log.e("CLOUDILLY", "@@@@@@ RECEIVED DEVICE: " + dict.toString());
		try {
			String device= dict.get("device").toString().toUpperCase();
			String action= dict.getInt("timestamp")== 0 ? "JOINED" : "LEFT";
			updateView(device + " " + action + " PUBLIC");
		} catch (JSONException e) { e.printStackTrace(); }
	}

	@Override
	public void socketReceivedPost(JSONObject dict) {
		Log.e("CLOUDILLY", "@@@@@@ RECEIVED POST: " + dict.toString());
		try {
			updateView(dict.get("device").toString().toUpperCase() + ": " + dict.getJSONObject("payload").get("msg"));
		} catch (JSONException e) { e.printStackTrace(); }
	}
}