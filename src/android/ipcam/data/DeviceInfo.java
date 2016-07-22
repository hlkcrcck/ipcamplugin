package com.edimax.edilife.ipcam.data;

/**
 * Created by Gregory on 2016/1/28.
 */
public class DeviceInfo {
	// Data
	public int DEV_RTSP_port                     = 0;
	public String DEV_RTSP_mpeg_path             = "";
	/**
	 * 3140 only has one profile path, and 5150 has three profile path
	 */
	public String DEV_RTSP_h264_profile0_path    = "";
	public String DEV_RTSP_h264_profile1_path    = "";
	public String DEV_RTSP_h264_profile2_path    = "";

	private static DeviceInfo instance = null;

	private DeviceInfo() {

	}

	private static synchronized void syncinit() {
		if (instance == null) {
			instance = new DeviceInfo();
		}
	}

	public static DeviceInfo getInstatnce() {
		if (instance == null) {
			syncinit();
		}
		return instance;
	}

	public static void clearInstance() {
		instance = null;
	}
}
