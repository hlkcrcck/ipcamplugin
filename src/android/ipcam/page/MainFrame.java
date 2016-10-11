package com.edimax.edilife.ipcam.page;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.os.Handler;

import com.devexpress.apptemplate.R;
import com.edimax.edilife.handle.DataParser;
import com.edimax.edilife.ipcam.data.DeviceInfo;
import com.edimax.edilife.service.LifeService;

import java.nio.ByteBuffer;

/**
 * Created by Gregory on 2016/1/27.
 */
public class MainFrame extends FrameLayout
		implements
		View.OnClickListener,
		Handler.Callback,
		LifeService.LifeCallbackListener {

	public static final String TAG      = "CustomAPI";
	public static final boolean isDebug = true;

	// Info
	public static final String MAC_3140 = "74DA3844F537";
	public static final String MAC_5150 = "74DA3844F537";
	public static final String USER     = "admin";
	public static final String PASSWORD = "1234";

	// Custom Job ID
	public static final int LIFE_GET_PNV_INFO           = 1;
	public static final int LIFE_SET_SMOOTH             = 2;
	public static final int LIFE_SET_SD                 = 3;
	public static final int LIFE_SET_HD                 = 4;

	// Message
	public static final int LIFE_PARSER_PNV_DONE = 1;

	// Connect type
	public static final int TYPE_IP     = 0;
	public static final int TYPE_MAC    = 1;

	// Widget
	private TextView mStatusView;

	// Video
	private LiveView mSurfaceView;
	private Bitmap bitmapCurrent;
	private boolean isStopDecoding      = false;
	private boolean isChangeResolution  = false;
	private byte[] byteRTSP;
	private int sizeRTSP    = 0;
	private byte[] oldbpData;
	private int oldSize     = 0;
	private int oldWidth    = 0;
	private int oldHeight   = 0;

	// Audio
	private AudioTrack mAudioTrack;
	private static final int LIFE_RTP_AUDIO_FORMAT_PCM_8000_1   = 0; // PCM
	private static final int LIFE_RTP_AUDIO_FORMAT_PCM_16000_2  = 1; // AAC
	private static final int LIFE_RTP_AUDIO_FORMAT_PCM_16000_1  = 2; // ADPCM

	// Data
	private String  mDUID;
	private String  mMAC;
	private String  mIP;
	private int     mMode;

	private Activity mActivity;
	private LifeService mSrvAPI;
	private Handler mHandler;

	public MainFrame(Activity activity, LifeService service) {
		super(activity);
		this.mActivity  = activity;
		this.mSrvAPI    = service;
		init();
	}

	private void init() {
		mHandler = new Handler(this);
		initData();
		findView();
		mSrvAPI.setLifeCallbackListener(this);
		doConnect(mMode);
	}

	private void initData() {
		mDUID   = MAC_3140;
		mMAC    = MAC_3140;
		mMode   = TYPE_MAC;
	}

	private void findView() {
		LayoutInflater.from(getContext()).inflate(R.layout.main_frame, this, true);

		// Init btn onClick
		Button btn;
		int mBtn_ids[] = new int[]{
				R.id.start, R.id.stop, R.id.logout, R.id.smooth, R.id.sd, R.id.hd
		};

		for (int i = 0; i < mBtn_ids.length; i++) {
			if ((btn = (Button) findViewById(mBtn_ids[i])) != null) {
				btn.setOnClickListener(this);
			}
		}

		// For Streaming
		mSurfaceView    = (LiveView) findViewById(R.id.stream_view);
		// For Show Status
		mStatusView     = (TextView) findViewById(R.id.status_view);

	}

	private void doConnect(int type) {
		switch (type) {
			case TYPE_MAC:
				Log.i(TAG, mSrvAPI.LIFE_CLOUD_IP);
				mSrvAPI.connect(mDUID, mSrvAPI.LIFE_SUPPORT_CUSTOMER, mSrvAPI.LIFE_CLOUD_IP, mMAC, mSrvAPI.LIFE_TUNNEL_RULE_AUTO);
				break;
			case TYPE_IP:
				//mSrvAPI.connectByIP(mDUID, mSrvAPI.LIFE_SUPPORT_CUSTOMER, mIP, WEB_PORT);
				break;
		}
		post(new Runnable() {
			@Override
			public void run() {
				mStatusView.setText(" CONNECTING.");
			}
		});
		if (isDebug) Log.i(TAG, "CONNECT TO THE DEVICE");
	}

	private void doLogin() {
		Log.i(TAG, mSrvAPI.LIFE_HTTP_CGI_EDILIFE);
		mSrvAPI.login(mDUID, USER, PASSWORD, mSrvAPI.LIFE_HTTP_CGI_EDILIFE, null);
		if (isDebug) Log.i(TAG, "TRY TO LOGIN");
	}


	private void doGetInfo() {

		mSrvAPI.customCGI(mDUID, LIFE_GET_PNV_INFO, mSrvAPI.LIFE_HTTP_CGI_LISTALL_PNV, null);
		if (isDebug) Log.i(TAG, "TRY TO GET PNV LIST");
	}

	/**
	 * If start RTSP, the H.264 Decode data will come through VideoCallBack,
	 * and if the model support the audio, audio data will come through AudioCallBack.
	 */
	private void startRTSP() {
		post(new Runnable() {
			@Override
			public void run() {
				mStatusView.setText(" STARTING RTSP.");
				mSurfaceView.setVisibility(VISIBLE);
				setBtnEnable(false);
			}
		});
		// We start RTSP by default path which is DEV_RTSP_h264_profile0_path
		mSrvAPI.startRTSP(mDUID, DeviceInfo.getInstatnce().DEV_RTSP_port, DeviceInfo.getInstatnce().DEV_RTSP_h264_profile0_path);
		if (isDebug) Log.i(TAG, "TRY TO START RTSP");
		isStopDecoding = false;
	}

	private void stopStreaming() {
		// Set Stop Flag is true, then stop video & audio
		isStopDecoding = true;
		mSrvAPI.stopRTSP(mDUID);
		if (isDebug) Log.i(TAG, "STOPPING RTSP");
		mStatusView.setText(" STOPPING RTSP.");
		mSurfaceView.stopShow();
		mSurfaceView.setVisibility(GONE);
		setBtnEnable(true);
	}

	private void setResolution(int resolution) {
		switch (resolution) {
			case LIFE_SET_SMOOTH:
				mSrvAPI.customCGI(mDUID, LIFE_SET_SMOOTH, mSrvAPI.LIFE_CGI_SET_VIDEO_SMOOTH, null);
				break;
			case LIFE_SET_SD:
				mSrvAPI.customCGI(mDUID, LIFE_SET_SD, mSrvAPI.LIFE_CGI_SET_VIDEO_SD, null);
				break;
			case LIFE_SET_HD:
				mSrvAPI.customCGI(mDUID, LIFE_SET_HD, mSrvAPI.LIFE_CGI_SET_VIDEO_HD, null);
				break;
		}

	}

	private void setBtnEnable(boolean enable) {
		findViewById(R.id.start).setEnabled(enable);
		findViewById(R.id.smooth).setEnabled(enable);
		findViewById(R.id.sd).setEnabled(enable);
		findViewById(R.id.hd).setEnabled(enable);
	}

	@Override
	public void onLifeJobCallback(String strDUID, int nJob, int nCustomID, byte[] bpData, int nDataSize, int nReturn) {
		if (nJob == mSrvAPI.LIFE_JOB_CONNECT) {
			if (nReturn == mSrvAPI.LIFE_ERR_SUCCESS || nReturn == mSrvAPI.LIFE_ERR_TUNNEL_OPENED) {
				if (isDebug) Log.i(TAG, "CONNECT SUCCESS");
				post(new Runnable() {
					@Override
					public void run() {
						mStatusView.setText(" CONNECTED.");
					}
				});
				// If the return code is SUCCESS or OPENED , means connect success then try login
				doLogin();
			} else {
				if (isDebug) Log.i(TAG, "CONNECT FAILED");
				post(new Runnable() {
					@Override
					public void run() {
						mStatusView.setText(" CONNECTION FAILED.");
					}
				});
				// Wait 2.5s to reconnect
				try {
					Thread.sleep(2500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					doConnect(mMode);
				}
			}
		} else if (nJob == mSrvAPI.LIFE_JOB_LOGIN) {
			if (nReturn == mSrvAPI.LIFE_ERR_SUCCESS) {
				// If login success, then start RTSP to get IP camera streaming
				if (isDebug) Log.i(TAG, "LOGIN SUCCESS");
				post(new Runnable() {
					@Override
					public void run() {
						mStatusView.setText(" LOGIN SUCCESS.");
					}
				});
				doGetInfo();
			} else {
				// Login failed, try to reconnect
				if (isDebug) Log.i(TAG, "LOGIN FAILED");
				post(new Runnable() {
					@Override
					public void run() {
						mStatusView.setText(" LOGIN FAILED.");
					}
				});
				doConnect(mMode);
			}
		} else if (nJob == mSrvAPI.LIFE_JOB_CUSTOM_CGI) {
			if (nReturn == mSrvAPI.LIFE_ERR_TUNNEL_CLOSED || nReturn == mSrvAPI.LIFE_ERR_TUNNEL_CLOSING) {
				// If tunnel closed, reconnect again
				doConnect(mMode);
			} else if (nReturn == mSrvAPI.LIFE_ERR_SUCCESS) {
				if (nCustomID == LIFE_GET_PNV_INFO) {
					if (isDebug) Log.i(TAG, "GET INFO SUCCESS");
					DataParser.parsing_pnv_listall_info(new String(bpData, 0, nDataSize), mHandler);
					startRTSP();
				} else if (nCustomID == LIFE_SET_SMOOTH || nCustomID == LIFE_SET_SD || nCustomID == LIFE_SET_HD) {
					if (isDebug) Log.i(TAG, "CHANGE RESOLUTION SUCCESS");
					isChangeResolution = true;
					startRTSP();
				}
			} else {
				// If job failed, wait 0.5, retry again.
				if (nCustomID == LIFE_GET_PNV_INFO) {
					if (isDebug) Log.i(TAG, "GET INFO FAILED");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						doGetInfo();
					}
				}
			}
		} else if (nJob == mSrvAPI.LIFE_JOB_START_RTSP) {
			if (nReturn == mSrvAPI.LIFE_ERR_TUNNEL_CLOSED || nReturn == mSrvAPI.LIFE_ERR_TUNNEL_CLOSING) {
				doConnect(mMode);
			} else if (nReturn == mSrvAPI.LIFE_ERR_SUCCESS) {
				post(new Runnable() {
					@Override
					public void run() {
						setBtnEnable(true);
						findViewById(R.id.start).setEnabled(false);
					}
				});
			} else {
				// RTSP Failed, retry again.
				if (isDebug) Log.i(TAG, "START RTSP FAILED");
				post(new Runnable() {
					@Override
					public void run() {
						mStatusView.setText(" START RTSP FAILED.");
					}
				});
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					startRTSP();
				}
			}
		} else if (nJob == mSrvAPI.LIFE_JOB_STOP_RTSP) {
			if (mAudioTrack != null) {
				mAudioTrack.stop();
				mAudioTrack.release();
				mAudioTrack = null;
			}
			if (!isStopDecoding) {
				post(new Runnable() {
					@Override
					public void run() {
						mStatusView.setText(" RTSP Interrupted Exception.");
						setBtnEnable(false);
					}
				});
				startRTSP();
			}
		} else if (nJob == mSrvAPI.LIFE_JOB_DISCONNECT) {
			if (isDebug) Log.i(TAG, "DISCONNECTED");
			post(new Runnable() {
				@Override
				public void run() {
					mStatusView.setText(" DISCONNECTED.");
					setBtnEnable(false);
				}
			});
			doConnect(mMode);
		}
	}

	@Override
	public void onLifeVideoCallback(String strDUID, byte[] bpData, int nDataSize, int nFormat, int nIFrame, int nWidth, int nHeight) {
		if (!isStopDecoding) {
			post(new Runnable() {
				@Override
				public void run() {
					mStatusView.setText(" STREAMING.");
				}
			});

			if (nDataSize == 0) {
				if (oldSize != 0) {
					bpData  = oldbpData;
					nWidth  = oldWidth;
					nHeight = oldHeight;
				} else {
					return;
				}
			}

			oldSize     = nDataSize;
			oldbpData   = bpData;

			if (nFormat == 0) {
				if (bitmapCurrent == null || isChangeResolution) {
					if (isDebug) Log.e(TAG, "CREATE BITMAP - WIDTH : " + nHeight + ", HEIGHT : " + nHeight);

					isChangeResolution = false;
					bitmapCurrent = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888);
					sizeRTSP = bitmapCurrent.getRowBytes() * bitmapCurrent.getHeight();
					byteRTSP = new byte[sizeRTSP];
				}
				System.arraycopy(bpData, 0, byteRTSP, 0, sizeRTSP);
				ByteBuffer buffer = ByteBuffer.wrap(byteRTSP, 0, sizeRTSP);
				bitmapCurrent.copyPixelsFromBuffer(buffer);

				// Start Draw
				mSurfaceView.setImageBitmap(bitmapCurrent);
				mSurfaceView.showImage();
			}
		}
	}

	@Override
	public void onLifeAudioCallback(String strDUID, byte[] bpData, int nDataSize, int nFormat) {
		if (mAudioTrack == null) {
			switch (nFormat) {
				case LIFE_RTP_AUDIO_FORMAT_PCM_8000_1:
					mAudioTrack = new AudioTrack(
							AudioManager.STREAM_MUSIC,
							8000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT,
							AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT),
							AudioTrack.MODE_STREAM
					);
					break;

				case LIFE_RTP_AUDIO_FORMAT_PCM_16000_2:
					mAudioTrack = new AudioTrack(
							AudioManager.STREAM_MUSIC,
							16000,
							AudioFormat.CHANNEL_CONFIGURATION_STEREO,
							AudioFormat.ENCODING_PCM_16BIT,
							AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT),
							AudioTrack.MODE_STREAM
					);
					break;

				case LIFE_RTP_AUDIO_FORMAT_PCM_16000_1:
					mAudioTrack = new AudioTrack(
							AudioManager.STREAM_MUSIC,
							16000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT,
							498,
							AudioTrack.MODE_STREAM
					);
					break;
			}

			try {
				mAudioTrack.play();
				mAudioTrack.write(bpData, 0, nDataSize);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			mAudioTrack.write(bpData, 0, nDataSize);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.start:
				startRTSP();
				break;
			case R.id.stop:
				stopStreaming();
				break;
			case R.id.smooth:
				/**
				 * stop RTSP before change resolution
				 */
				stopStreaming();
				setResolution(LIFE_SET_SMOOTH);
				break;
			case R.id.sd:
				stopStreaming();
				setResolution(LIFE_SET_SD);
				break;
			case R.id.hd:
				stopStreaming();
				setResolution(LIFE_SET_HD);
				break;
			case R.id.logout:
				mActivity.finish();
				break;
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case LIFE_PARSER_PNV_DONE:
				setBtnEnable(true);
				break;
		}
		return false;
	}
}
