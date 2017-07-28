package com.heytz.mxsdkwrapper;

import android.content.Context;
import android.util.Log;

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

/**
 * This class starts transmit to activation
 */
public class mxsdkwrapper extends CordovaPlugin {

    private static String TAG = "=====mxsdkwrapper====";
    private CallbackContext easyLinkCallbackContext;
    private EasyLink elink;
    private String _SERV_NAME = "_easylink._tcp.local.";
    private MDNS mdns;

    /**
     * Step 2.1 FTC Call back, process the response from MXChip Model.
     */
//    private FTC_Listener ftcListener;//new FTCLisenerExtension(easyLinkCallbackContext);
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        elink = new EasyLink(cordova.getActivity().getApplicationContext());
        mdns = new MDNS(cordova.getActivity().getApplicationContext());
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("setDeviceWifi")) {
            String wifiSSID = args.getString(0);
            String wifiKey = args.getString(1);
            if (wifiSSID == null || wifiSSID.length() == 0 || wifiKey == null || wifiKey.length() == 0) {
                Log.e(TAG, "arguments error ===== empty");
                return false;
            }
            easyLinkCallbackContext = callbackContext;
            this.startEl(wifiSSID, wifiKey);
            startMDNS();
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
     * ????????
     */
    private void startEl(String wifiSSID, String wifiKey) {
        EasyLinkParams easylinkPara = new EasyLinkParams();
        easylinkPara.ssid = wifiSSID.trim();
        easylinkPara.password = wifiKey.toString().trim();
        easylinkPara.runSecond = 60000;
        easylinkPara.sleeptime = 10;

        elink.startEasyLink(easylinkPara, new EasyLinkCallBack() {
            @Override
            public void onSuccess(int code, String message) {
                Log.d(TAG + "startEasyLink", code + message.toString());
            }

            @Override
            public void onFailure(int code, String message) {
                Log.d(TAG + "startEasyLink", code + message.toString());
                if (easyLinkCallbackContext != null) {
                    easyLinkCallbackContext.error("startEasyLink error");
                    stopEl();
                }
            }
        });
    }

    /**
     * ????????
     */
    private void stopEl() {

        elink.stopEasyLink(new EasyLinkCallBack() {
            @Override
            public void onSuccess(int code, String message) {
                Log.d(TAG + "stopEasyLink", code + message.toString());

            }

            @Override
            public void onFailure(int code, String message) {
                Log.d(TAG + "stopEasyLink", code + message.toString());
            }
        });
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
                stopEl();
                if (easyLinkCallbackContext != null) {
                    easyLinkCallbackContext.error("mdns failure" + message);
                }
            }

            @Override
            public void onDevicesFind(int code, JSONArray deviceStatus) {
                super.onDevicesFind(code, deviceStatus);
                if (deviceStatus.length() > 0 && easyLinkCallbackContext != null) {
                    try {
                        JSONObject device = deviceStatus.getJSONObject(0);
                        Log.d(TAG + "startMDNS", code + deviceStatus.toString());
                        JSONObject result = new JSONObject();
                        result.put("name", device.getString("Name"));
                        result.put("ip", device.getString("IP"));
                        result.put("mac", device.getString("MAC").replaceAll(":", ""));
                        easyLinkCallbackContext.success(result);
                        stopEl();
                        stopMDNS();
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
        stopEl();
        stopMDNS();
        easyLinkCallbackContext = null;
    }

    private void resetEasyLink() {
        stop();
    }

}
