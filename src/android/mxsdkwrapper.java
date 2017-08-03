package com.heytz.mxsdkwrapper;

import android.content.Context;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;

import com.mxchip.easylink.EasyLinkAPI;
import com.mxchip.easylink.FTCListener;

import io.fogcloud.sdk.easylink.api.EasyLink;
import io.fogcloud.sdk.easylink.helper.EasyLinkCallBack;
import io.fogcloud.sdk.easylink.helper.EasyLinkParams;
import io.fogcloud.sdk.mdns.api.MDNS;
import io.fogcloud.sdk.mdns.helper.SearchDeviceCallBack;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class starts transmit to activation
 */
public class mxsdkwrapper extends CordovaPlugin {

    private static String TAG = "=====mxsdkwrapper====";
    private CallbackContext easyLinkCallbackContext;
    private Map<String, JSONObject> deviceList;
    private String _SERV_NAME = "_easylink._tcp.local.";
    private MDNS mdns;
    private Handler elHandler;
    private EasyLinkAPI elapi;
    private Runnable startElRunnable;
    private FTCListener ftcListener = new FTCListener() {
        @Override
        public void onFTCfinished(String ip,
                                  String data) {
            elapi.stopEasyLink();
            if (!"".equals(data)) {
                JSONObject jsonObj;
                try {

/* {
  "T": "Current Configuration", "N": "EMW3162(4D994C)", "C":
    [{
      "N": "MICO SYSTEM", "C":
        [{"N": "Device Name", "C": "EMW3162 Module", "P": "RW"},
          {"N": "Bonjour", "C": true, "P": "RW"},
          {"N": "RF power save", "C": false, "P": "RW"},
          {"N": "MCU power save", "C": false, "P": "RW"},
          {
            "N": "Detail", "C":
            [{
              "N": "", "C":
                [{"N": "Firmware Rev.", "C": "MICO_FOG_1_0", "P": "RO"},
                  {"N": "Hardware Rev.", "C": "3162", "P": "RO"},
                  {"N": "MICO OS Rev.", "C": "31620002.035", "P": "RO"},
                  {
                    "N": "RF Driver Rev.",
                    "C": "wl0: Sep 10 2014 11:28:46 version 5.90.230.10 FWID\u0002\bEMW3162(4D994C)",
                    "P": "RO"
                  },
                  {"N": "Model", "C": "EMW3162", "P": "RO"},
                  {"N": "Manufacturer", "C": "MXCHIP Inc.", "P": "RO"},
                  {"N": "Protocol", "C": "com.mxchip.fog", "P": "RO"}]
            },
              {
                "N": "WLAN", "C":
                [{"N": "BSSID", "C": "34:96:72:6C:15:DB", "P": "RO"},
                  {"N": "Channel", "C": 11, "P": "RO"},
                  {"N": "Security", "C": "WPA2 AES", "P": "RO"},
                  {"N": "PMK", "C": "C93E3DDB4D6B47CAF3CD89D18E73B81C19095E1A40E3D75450131D3CA2858023", "P": "RO"}]
              }]
          }]
    },
      {
        "N": "WLAN", "C":
        [{"N": "Wi-Fi", "C": "Heytz_WDR", "P": "RW"},
          {"N": "Password", "C": "523618++", "P": "RW"},
          {"N": "DHCP", "C": true, "P": "RW"},
          {"N": "IP address", "C": "192.168.1.151", "P": "RW"},
          {"N": "Net Mask", "C": "255.255.255.0", "P": "RW"},
          {"N": "Gateway", "C": "192.168.1.1", "P": "RW"},
          {"N": "DNS Server", "C": "192.168.1.1", "P": "RW"}]
      },
      {
        "N": "MCU IOs", "C":
        [{"N": "Baurdrate", "C": 115200, "P": "RW", "S": [9600, 19200, 38400, 57600, 115200]}]
      },
      {
        "N": "Cloud info", "C":
        [{"N": "activated", "C": false, "P": "RO"},
          {"N": "connected", "C": false, "P": "RO"},
          {"N": "rom version", "C": "v1.0.1", "P": "RO"},
          {"N": "device_id", "C": "null", "P": "RW"},
          {
            "N": "Cloud settings", "C":
            [{
              "N": "Authentication", "C":
                [{"N": "loginId", "C": "admin", "P": "RW"},
                  {"N": "devPasswd", "C": "88888888", "P": "RW"},
                  {"N": "userToken", "C": "", "P": "RW"}]
            }]
          }]
      }],
  "PO": "com.mxchip.fog",
  "HD": "3162",
  "FW": "MICO_FOG_1_0"
}*/

                    jsonObj = new JSONObject(data);
                    String deviceName = jsonObj.getString("N");
                    final String deviceIP = jsonObj.getJSONArray("C")
                            .getJSONObject(1).getJSONArray("C")
                            .getJSONObject(3).getString("C");
                    Log.i(TAG, "findedDeviceIP:" + deviceIP);
                    final String deviceMac = "C89346"
                            + deviceName.substring(
                            deviceName.indexOf("(") + 1,
                            deviceName.length() - 1);
                    JSONObject result = new JSONObject();
                    result.put("name", deviceName);
                    result.put("ip", deviceIP);
                    result.put("mac", deviceMac);
                    if (easyLinkCallbackContext != null)
                        easyLinkCallbackContext.success(result);
                    stopElFTC();
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                    easyLinkCallbackContext.error("parse JSON obj error");
                }
            } else {
                Log.e(TAG, "socket data is empty!");
                easyLinkCallbackContext.error("FTC socket data empty");
            }
        }

        @Override
        public void isSmallMTU(int MTU) {
        }
    };

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mdns = new MDNS(cordova.getActivity().getApplicationContext());
        deviceList = new HashMap<String, JSONObject>();
        elapi = new EasyLinkAPI(cordova.getActivity().getApplicationContext());
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("setDeviceWifi")) {
            final String wifiSSID = args.getString(0);
            final String wifiKey = args.getString(1);
            if (wifiSSID == null || wifiSSID.length() == 0 || wifiKey == null || wifiKey.length() == 0) {
                Log.e(TAG, "arguments error ===== empty");
                return false;
            }
            easyLinkCallbackContext = callbackContext;
            deviceList.clear();
            startMDNS();
            elHandler = new Handler();
            startElRunnable = new Runnable() {
                @Override
                public void run() {
                    transmitSettings(wifiSSID, wifiKey);
                    elHandler = null;
                }
            };
            elHandler.postDelayed(startElRunnable, 10 * 1000);
            return true;
        }

        if (action.equals("dealloc")) {
            try {
                stop();
                callbackContext.success();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return true;
        }
        return false;
    }


    /**
     * Step1. Call FTC Service to start transmit settings.
     *
     * @param wifiSSID @desc wifi ssid
     * @param wifiKey  @desc corresponding wifi key
     */
    private void transmitSettings(String wifiSSID, String wifiKey) {
        Log.i(TAG, " Step1. Call FTC Service to transmit settings. SSID = " + wifiSSID + ", Password = " + wifiKey);
        if (wifiSSID != null && wifiSSID.length() > 0 && wifiKey != null && wifiKey.length() > 0) {
            stopElFTC();
            elapi.startFTC(wifiSSID, wifiKey, ftcListener);
        } else {
            easyLinkCallbackContext.error("args error");
        }
    }

    private void stopElFTC() {
        elapi.stopFTC();
        elapi.stopEasyLink();
        elapi=null;
        elapi = new EasyLinkAPI(cordova.getActivity().getApplicationContext());
    }

    private void startMDNS() {
        mdns.startSearchDevices(_SERV_NAME, new SearchDeviceCallBack() {
            @Override
            public void onSuccess(int code, String message) {
                super.onSuccess(code, message);
                Log.d(TAG + "startMDNS", code + message.toString());
            }

            @Override
            public void onFailure(int code, String message) {
                super.onFailure(code, message);
                Log.d(TAG + "startMDNS", code + message.toString());
                stopElFTC();
                if (easyLinkCallbackContext != null) {
                    easyLinkCallbackContext.error("mdns failure" + message);
                }
            }

            /**
             *
             * @param code
             * @param deviceStatus [{"Name":"EMW3080B Module#1009AD","IP":"192.168.1.131","Port":8000,
             *                     "MAC":"B0:F8:93:10:09:AD","Firmware Rev":"MICO_MQTT_Client_1",
             *                     "Hardware Rev":"3080B","MICO OS Rev":"3080B002.004","Model":"EMW3080B",
             *                     "Protocol":"com.mxchip.mqtt","Manufacturer":"MXCHIP Inc.","Seed":"13"},
             *                     {"Name":"EMW3080B Module#100998","IP":"192.168.1.176","Port":8000,
             *                     "MAC":"B0:F8:93:10:09:98","Firmware Rev":"MICO_MQTT_Client_1",
             *                     "Hardware Rev":"3080B","MICO OS Rev":"3080B002.004","Model":"EMW3080B",
             *                     "Protocol":"com.mxchip.mqtt","Manufacturer":"MXCHIP Inc.","Seed":"5"}]
             */
            @Override
            public void onDevicesFind(int code, JSONArray deviceStatus) {
                super.onDevicesFind(code, deviceStatus);
                Log.d(TAG + "startMDNS", "device count:\n" + deviceStatus.length() + " code:" +
                        code + "\n" + deviceStatus.toString());
                if (deviceStatus.length() > 0) {
                    try {
                        for (int i = 0; i < deviceStatus.length(); i++) {
                            JSONObject device = deviceStatus.getJSONObject(i); //
                            String mac = device.getString("MAC");
                            if (!deviceList.containsKey(mac)) {
                                if (elHandler == null) {
                                    Log.d(TAG + "", "search a device Mac:" + mac);
                                    JSONObject result = new JSONObject();
                                    result.put("name", device.getString("Name"));
                                    result.put("ip", device.getString("IP"));
                                    result.put("mac", device.getString("MAC").replaceAll(":", ""));
                                    if (easyLinkCallbackContext != null)
                                        easyLinkCallbackContext.success(result);
                                    stop();
                                } else {
                                    deviceList.put(mac, device);
                                }
                            } else {
//                                if (elHandler != null) {//如果这个设备是先点击手机配网，再让设备处罚配网。
//                                    deviceList.remove(mac);
//                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void stopMDNS() {
        mdns.stopSearchDevices(new SearchDeviceCallBack() {
            @Override
            public void onSuccess(int code, String message) {
                super.onSuccess(code, message);
            }
        });
    }

    private void stop() {
        stopElFTC();
        stopMDNS();
        easyLinkCallbackContext = null;
        if (elHandler != null && startElRunnable != null) {
            elHandler.removeCallbacks(startElRunnable);
            elHandler = null;
        }
    }
}
