package com.edimax.edilife.handle;

import android.os.Message;
import android.util.Log;

import com.edimax.edilife.ipcam.data.DeviceInfo;
import com.edimax.edilife.ipcam.page.MainFrame;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import android.os.Handler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Gregory on 2016/1/28.
 * The Parser which can parsing the string value that we want from device in XML
 * If we usedd LIFE_HTTP_CGI_LISTALL_PNV (/camera-cgi/pnv/param.cgi?action=listall) CGI,
 * the receive XML will look like as below :
 * <IPCamera>
 *   <usergroup>admin</usergroup>
 *   <supportpnv>1</supportpnv>
 *   <cgiVersion>1.01</cgiVersion>
 *   <SystemInfo>
 *      <vendor id="0">EDIMAX</vendor>
 *      <model id="70">IC-7112W</model>
 *      <version>v1.05</version>
 *   </SystemInfo>
 *   <PnVConfig>
 *       <Video>
 *           ............
 *       </Video>
 *       <PT>
 *           ............
 *       </PT>
 *       <RTSP>
 *          <rtspPort>554</rtspPort>
 *          <rtspMJPEGPath id="profile0">ipcam_mjpeg</rtspMJPEGPath>
 *          <rtspH264Path id="profile0">ipcam_h264</rtspH264Path>
 *          <rtspH264Path id="profile1">ipcam_h264s1</rtspH264Path>
 *          <rtspH264Path id="profile2">ipcam_h264s2</rtspH264Path>
 *          <RTPPortRangeFrom>50000</RTPPortRangeFrom>
 *          <RTPPortRangeTo>60000</RTPPortRangeTo>
 *          <rtspAuth>1</rtspAuth>
 *       </RTSP>
 *   </PnVConfig>
 * </IPCamera>
 */
public class DataParser {


	/**
	 * Since, we are only interested in device's RTSP info, therefore in this function,
	 * we focus on the Elements which TAG NAME is RTSP, and find the item info which are
	 * "rtspPort", "rtspMJPEGPath", "rtspH264Path".
	 */
	public static void parsing_pnv_listall_info(String strXML, Handler handler) {

		if ((strXML == null) ||
				(strXML.length() == 0) ||
				(strXML.indexOf("supportpnv") == -1)) {
			return;
		}

		DeviceInfo info = DeviceInfo.getInstatnce();

		try {
			DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder         = factory.newDocumentBuilder();
			InputSource is                  = new InputSource(new StringReader(strXML));
			Document doc                    = builder.parse(is);
			NodeList nl;
			Node node;
			int nIdx;

			int dev_h264_path_count = 0;
			nl = doc.getElementsByTagName("RTSP");
			if ((nl != null) && (nl.getLength() > 0)) {
				if ((nl = nl.item(0).getChildNodes()) != null) {
					for (nIdx = 0; nIdx < nl.getLength(); nIdx++) {
						if (nl.item(nIdx).getNodeType() == Node.ELEMENT_NODE) {
							if (nl.item(nIdx).getNodeName()
									.compareToIgnoreCase("rtspPort") == 0) {
								if ((node = nl.item(nIdx).getFirstChild()) != null) {
									info.DEV_RTSP_port = Integer.parseInt(node.getNodeValue());
								}
							}
							if (nl.item(nIdx).getNodeName()
									.compareToIgnoreCase("rtspMJPEGPath") == 0) {
								if ((node = nl.item(nIdx).getFirstChild()) != null) {
									info.DEV_RTSP_mpeg_path = node.getNodeValue() + ".sdp";
								}
							}
							if (nl.item(nIdx).getNodeName().compareToIgnoreCase("rtspH264Path") == 0) {
								if ((node = nl.item(nIdx).getFirstChild()) != null) {

									if (dev_h264_path_count == 0) {
										info.DEV_RTSP_h264_profile0_path = node.getNodeValue() + ".sdp";
									} else if (dev_h264_path_count == 1) {
										info.DEV_RTSP_h264_profile1_path = node.getNodeValue() + ".sdp";
									} else {
										info.DEV_RTSP_h264_profile2_path = node.getNodeValue() + ".sdp";
									}
									dev_h264_path_count++;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (MainFrame.isDebug) {
				Log.i(MainFrame.TAG,
						"============DEVICE INFO===============" +
								"\n" +
								"RTSP PORT : " +
								DeviceInfo.getInstatnce().DEV_RTSP_port +
								"\n" +
								"H264_PROFILE_0 : " +
								DeviceInfo.getInstatnce().DEV_RTSP_h264_profile0_path +
								"\n" +
								"H264_PROFILE_1 : " +
								DeviceInfo.getInstatnce().DEV_RTSP_h264_profile1_path +
								"\n" +
								"H264_PROFILE_2 : " +
								DeviceInfo.getInstatnce().DEV_RTSP_h264_profile2_path +
								"\n" +
								"MPEG_PATH : " +
								DeviceInfo.getInstatnce().DEV_RTSP_mpeg_path +
								"\n" +
								"============DEVICE INFO==============="
				);
			}
			Message msg = handler.obtainMessage(MainFrame.LIFE_PARSER_PNV_DONE);
			handler.sendMessage(msg);
		}
	}
}
