package com.edimax.edilife;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.FrameLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import com.edimax.edilife.ipcam.data.DeviceInfo;
import com.edimax.edilife.ipcam.page.MainFrame;
import com.edimax.edilife.service.LifeService;

import com.vestel.smarthomedemo.R;

/**
 * Created by Gregory on 2016/1/27.
 */
public class MainActivity extends Activity {

	private LifeService mSrvAPI;
	private FrameLayout mLayRoot;
	private MainFrame   mMainFrame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		// Bind LifeService
		Intent aIntent = new Intent(LifeService.class.getName());
		aIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		aIntent.setPackage(this.getPackageName());
		bindService(aIntent, m_SrvConnection, Context.BIND_AUTO_CREATE);

		mLayRoot = (FrameLayout)findViewById(R.id.lay_root);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	// Service Connection
	private ServiceConnection m_SrvConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mSrvAPI = ((LifeService.loadBinder) service).getService();
			addFrame();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mSrvAPI = null;
		}
	};

	private void addFrame() {
		mLayRoot.removeAllViewsInLayout();
		mMainFrame = new MainFrame(this, mSrvAPI);
		mLayRoot.addView(mMainFrame);
	}

	@Override
	protected void onDestroy() {
		// Device disconnected
		mSrvAPI.disconnect(MainFrame.MAC_5150);
		// Unbind
		unbindService(m_SrvConnection);
		// Clear DeviceInfo
		DeviceInfo.clearInstance();
		super.onDestroy();
	}

	@Override
	public void finish() {
		super.finish();
	}
}
