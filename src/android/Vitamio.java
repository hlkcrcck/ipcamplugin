package com.edimax.edilife;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

public class Vitamio extends CordovaPlugin {
	public static final String ACTION_PLAY_AUDIO = "playAudio";
	public static final String ACTION_PLAY_VIDEO = "playVideo";

	private static final int ACTIVITY_CODE_PLAY_MEDIA = 7;

	private CallbackContext callbackContext;
	private BroadcastReceiver receiver;

	private static final String TAG = "VitamioPlugin";

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		JSONObject options = null;

		try {
			options = args.getJSONObject(1);
		} catch (JSONException e) {
			// Developer provided no options. Leave options null.
		}

		if (ACTION_PLAY_AUDIO.equals(action)) {
			return playAudio(args.getString(0), options);
		} else if (ACTION_PLAY_VIDEO.equals(action)) {
			return playVideo(args.getString(0), options);
		} else {
			callbackContext.error("vitamio." + action + " is not a supported method.");
			return false;
		}
	}

	private boolean playAudio(String url, JSONObject options) throws JSONException {
		Intent i = new Intent("ACTION_PLAY_VIDEO");
		this.cordova.startActivityForResult((CordovaPlugin) this,i, 0);
		return true;
	}
	private boolean playVideo(String url, JSONObject options) throws JSONException {
		final CordovaInterface cordovaObj = cordova;
		final CordovaPlugin plugin = this;
		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				Intent i = new Intent("ACTION_PLAY_VIDEO");
				cordovaObj.startActivityForResult(plugin,i, 0);
			}
		});
		return true;
	}


}
