package com.edimax.edilife.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class LifeService extends Service {

	/**
	 * Life API call back interface, implements these three callbacks to receive data from service.
	 */
	public interface LifeCallbackListener {
		void onLifeJobCallback(String strDUID, int nJob, int nCustomID, byte[] bpData, int nDataSize, int nReturn);

		void onLifeVideoCallback(String strDUID, byte[] bpData, int nDataSize, int nFormat, int nIFrame, int nWidth, int nHeight);

		void onLifeAudioCallback(String strDUID, byte[] bpData, int nDataSize, int nFormat);
	}

	private LifeCallbackListener m_myCallback;

	//=================================================
	// CGI
	//=================================================
	public static final  String LIFE_HTTP_CGI_EDILIFE        = "/edilife.cgi";
	public static final  String LIFE_HTTP_CGI_LISTALL_PNV    = "/camera-cgi/pnv/param.cgi?action=listall";
	public static final  String LIFE_HTTP_CGI_LISTALL        = "/camera-cgi/admin/param.cgi?action=listall";
	public static final  String LIFE_CGI_SET_VIDEO_SMOOTH    = "/camera-cgi/admin/param.cgi?action=update&Video_H264_width=320&Video_H264_height=240&Video_H264_bitrate=122880&Video_H264_FPS=10";
	public static final  String LIFE_CGI_SET_VIDEO_SD        = "/camera-cgi/admin/param.cgi?action=update&Video_H264_width=640&Video_H264_height=480&Video_H264_bitrate=245760&Video_H264_FPS=10";
	public static final  String LIFE_CGI_SET_VIDEO_HD        = "/camera-cgi/admin/param.cgi?action=update&Video_H264_width=1280&Video_H264_height=720&Video_H264_bitrate=552960&Video_H264_FPS=15";

	// Tunnel Rules
	public final int LIFE_TUNNEL_RULE_AUTO          = 0;    // LAN + Cloud
	public final int LIFE_TUNNEL_RULE_CLOUD_ONLY    = 1;    // P2P or Relay
	public final int LIFE_TUNNEL_RULE_P2P_ONLY      = 2;    // P2P
	public final int LIFE_TUNNEL_RULE_RELAY_ONLY    = 3;    // Relay
	public final int LIFE_TUNNEL_RULE_LAN_P2P       = 4;    // LAN or P2P
	public final int LIFE_TUNNEL_RULE_LAN_RELAY     = 5;    // LAN or Relay
	public final int LIFE_TUNNEL_RULE_LAN_ONLY      = 6;    // LAN

	// Tunnel Type
	public final int LIFE_TUNNEL_TYPE_NONE          = 0;
	public final int LIFE_TUNNEL_TYPE_LAN           = 1;
	public final int LIFE_TUNNEL_TYPE_P2P           = 2;
	public final int LIFE_TUNNEL_TYPE_RELAY         = 3;
	public final int LIFE_TUNNEL_TYPE_IP            = 4;

	// Tunnel State
	public final int LIFE_TUNNEL_STATE_CLOSED       = 0;
	public final int LIFE_TUNNEL_STATE_OPENED       = 1;
	public final int LIFE_TUNNEL_STATE_OPENING      = 2;
	public final int LIFE_TUNNEL_STATE_CLOSING      = 3;

	// Pan & Tilt Direction
	public static final int LIFE_PT_MOVE_UP         = 0; // "up"
	public static final int LIFE_PT_MOVE_DOWN       = 1; // "down"
	public static final int LIFE_PT_MOVE_LEFT       = 2; // "left"
	public static final int LIFE_PT_MOVE_RIGHT      = 3; // "right"
	public static final int LIFE_PT_MOVE_UP_LEFT    = 4; // "upleft"
	public static final int LIFE_PT_MOVE_DOWN_LEFT  = 5; // "downleft"
	public static final int LIFE_PT_MOVE_UP_RIGHT   = 6; // "upright"
	public static final int LIFE_PT_MOVE_DOWN_RIGHT = 7; // "downright"
	public static final int LIFE_PT_MOVE_CENTER     = 8; // " center",  /9 RESET_PT/10 HOME/11 STOP/12 ORIGIN


	//=================================================
	// Jobs
	//=================================================
	public final int LIFE_JOB_NONE                  = 0;
	public final int LIFE_JOB_CONNECT               = 1;
	public final int LIFE_JOB_DISCONNECT            = 2;
	public final int LIFE_JOB_RELEASE               = 3;

	public final int LIFE_JOB_LOGIN                 = 10;
	public final int LIFE_JOB_CUSTOM_CGI            = 11;
	public final int LIFE_JOB_GET_ALL_INFO          = 12;
	public final int LIFE_JOB_GET_WIFI_LIST         = 13;

	public final int LIFE_JOB_SET_TIMEZONE          = 20;
	public final int LIFE_JOB_SET_WIFI_CONFIG       = 21;

	public final int LIFE_JOB_SEARCH                = 90;
	public final int LIFE_JOB_KEEP_ALIVE            = 91;

	public final int LIFE_JOB_GET_SNAPSHOT          = 100;
	public final int LIFE_JOB_START_RTSP            = 101;
	public final int LIFE_JOB_STOP_RTSP             = 102;

	public final int LIFE_JOB_PAN_TILT              = 110;
	public final int LIFE_JOB_GET_EVENT_LIST        = 111;
	public final int LIFE_JOB_GET_EVENT_FILE        = 112;

	public final int LIFE_JOB_START_SEND_AUDIO      = 150;
	public final int LIFE_JOB_SEND_AUDIO            = 151;
	public final int LIFE_JOB_STOP_SEND_AUDIO       = 152;

	public final int LIFE_JOB_START_READ_AVI_FILE   = 160;
	public final int LIFE_JOB_STOP_READ_AVI_FILE    = 161;

	//=================================================
	// Return Code
	//=================================================
	public final int LIFE_ERR_SUCCESS               = 0;

	public final int LIFE_ERR_RELEASE               = -1;
	public final int LIFE_ERR_INVLIDE_JOB           = -2;
	public final int LIFE_ERR_SIZE                  = -3;
	public final int LIFE_ERR_INDEX                 = -4;
	public final int LIFE_ERR_PARSER                = -5;
	public final int LIFE_ERR_FORMAT                = -6;
	public final int LIFE_ERR_PNV_V1                = -7;

	public final int LIFE_ERR_DEVICE_NOT_FOUND      = -10;
	public final int LIFE_ERR_CLOUD_NOT_FOUND       = -11;
	public final int LIFE_ERR_CLOUD_NO_RESPONSE     = -12;
	public final int LIFE_ERR_DEVICE_FW_OLD         = -13;
	public final int LIFE_ERR_CLIENT_FW_OLD         = -14;
	public final int LIFE_ERR_DEVICE_FW_UPGRADING   = -15;

	public final int LIFE_ERR_TUNNEL_CLOSED         = -20;
	public final int LIFE_ERR_TUNNEL_OPENED         = -21;
	public final int LIFE_ERR_TUNNEL_OPENING        = -22;
	public final int LIFE_ERR_TUNNEL_CLOSING        = -23;
	public final int LIFE_ERR_CONNECTION_FAILED     = -24;
	public final int LIFE_ERR_TIMEOUT               = -25;

	public final int LIFE_ERR_PASSWORD              = -30;
	public final int LIFE_ERR_CMD_NOT_SUPPORT       = -31;
	public final int LIFE_ERR_TOO_MANY              = -32;
	public final int LIFE_ERR_DIGEST                = -33;

	public final int LIFE_ERR_OPEN_FILE             = -40;
	public final int LIFE_ERR_FILE_INFO             = -41;

	public native int init(String strMyClassPathName, String strCallBackName, String strCallBackVideoName, String strCallBackAudioName, String strCallBackVideoDataName, String strCallBackAudioDataName);
	public native void release();
	public native void customCGI(String strDevUID, int nCustomID, String strCGI, String strPostData);
	public native void search();
	public native void connect(String strDevUID, String strVendor, String strCloudIP, String strMacAddr, int nLIFE_TUNNEL_RULE);
	public native void connectByIP(String strDevUID, String strVendor, String strIPAddr, int nWebPort);
	public native void disconnect(String strDevUID);
	public native void login(String strDevUID, String strUser, String strPassword, String strCGI, String strPostData);
	public native void getSnapshot(String strDevUID);
	public native void startRTSP(String strDevUID, int nRTSPPort, String strRTSPPath);
	public native void stopRTSP(String strDevUID);
	public native void startSend_Audio(String strDevUID, byte[] bpData, int nDataSize, int nLIFE_AUDIO_OUT_FORAMT, int nPort);
	public native void sendAudio(String strDevUID, byte[] bpData, int nDataSize, int nLIFE_AUDIO_OUT_FORAMT);
	public native void stopSendAudio(String strDevUID, int nLIFE_AUDIO_OUT_FORAMT);
	public native void panTilt(String strDevUID, int nLIFE_PT_MOVE);
	public native void getEventList(String strDevUID);
	public native void getEventFile(String strDevUID, String strRecPath);
	public native void startReadAVIFile(String strDevUID, String strFullName);
	public native void stopReadAVIFile();


	// Cloud Servers
	public static final String LIFE_CLOUD_DOMAIN        = "www.myedimax.com";
	public static final String LIFE_CLOUD_IP            = "122.248.252.67";
	public static final String LIFE_SUPPORT_CUSTOMER    = "EDIMAX";

	private static byte[] m_bsVideo = new byte[1];
	private static byte[] m_bsAudio = new byte[1];
	private static byte[] m_bsImage = new byte[1];
	private static int m_nImgLock   = 0;

	private final IBinder m_Binder = new loadBinder();

	static {
		try {
			System.loadLibrary("ffmpeg");
			System.loadLibrary("LifeAPI");
		} catch (UnsatisfiedLinkError e) {
			Log.e("LifeService", e.getLocalizedMessage());
		}
	}

	public LifeService() {


	}

	@Override
	public void onCreate() {
		super.onCreate();

	}

	@Override
	public IBinder onBind(Intent intent) {
		init("com/edimax/edilife/service/LifeService", "LifeJobCallback", "LifeVideoCallback", "LifeAudioCallback", "m_bsVideo", "m_bsAudio");
		return m_Binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {

		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		this.release();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	public class loadBinder extends Binder {
		public LifeService getService() {
			return LifeService.this;
		}
	}


	/**
	 * @param target, Set call back listener
	 */
	public void setLifeCallbackListener(Object target) {
		// on click listener
		if (target instanceof LifeCallbackListener) {
			m_myCallback = (LifeCallbackListener) target;
		}

	}

	public void LifeJobCallback(String strDUID, int nJob, int nCustomID, byte[] bpData, int nDataSize, int nReturn) {

		if (m_myCallback != null) {
			m_myCallback.onLifeJobCallback(strDUID, nJob, nCustomID, bpData, nDataSize, nReturn);
		}
	}

	public void LifeVideoCallback(String strDUID, byte[] bpData, int nDataSize, int nFormat, int nIFrame, int nWidth, int nHeight) {

		if (m_nImgLock != 0) return;

		m_nImgLock = 1;

		if (nDataSize > m_bsVideo.length) {

			m_bsVideo = null;
			m_bsVideo = new byte[nDataSize + 1024];

			m_bsImage = null;
			m_bsImage = new byte[nDataSize + 1024];

			m_nImgLock = 0;
			nDataSize = 0;
		} else {
			System.arraycopy(m_bsVideo, 0, m_bsImage, 0, nDataSize);
		}

		if (m_myCallback != null) {
			m_myCallback.onLifeVideoCallback(strDUID, bpData, nDataSize, nFormat, nIFrame, nWidth, nHeight);
		}

		m_nImgLock = 0;
	}

	public void LifeAudioCallback(String strDUID, byte[] bpData, int nDataSize, int nFormat) {

		if (nDataSize > m_bsAudio.length) {

			m_bsAudio = null;
			m_bsAudio = new byte[nDataSize];
			return;
		}
		if (m_myCallback != null) {
			m_myCallback.onLifeAudioCallback(strDUID, bpData, nDataSize, nFormat);
		}
	}
}
