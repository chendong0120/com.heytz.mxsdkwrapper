package com.heytz.mxsdkwrapper;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.mxchip.easylink.EasyLinkAPI;
import com.mxchip.easylink.FTCListener;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class starts transmit to activation
 */
public class mxsdkwrapper extends CordovaPlugin {

    private static String TAG = "=====mxsdkwrapper.class====";
    private CallbackContext easyLinkCallbackContext;
    private Context context;
    //    private FTC_Service ftcService;
    private String userName;
    private String deviceLoginID;
    private String devicePassword;
    //  private String appSecretKey;
    //  private int easylinkVersion;
    private int activateTimeout;
    private String activatePort;
    private EasyLinkAPI elapi;


    /**
     * Step 2.1 FTC Call back, process the response from MXChip Model.
     */
//    private FTC_Listener ftcListener;//new FTCLisenerExtension(easyLinkCallbackContext);
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getActivity().getApplicationContext();
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
            this.transmitSettings(wifiSSID, wifiKey);
            return true;
        }
        if (action.equals("dealloc")) {
//            final EasyLinkAPI elapi = new EasyLinkAPI(context);
            try {
                stop();
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
//            final EasyLinkAPI elapi = new EasyLinkAPI(context);
            resetEasyLink();
            elapi.startFTC(wifiSSID, wifiKey, new FTCListener() {
                @Override
                public void onFTCfinished(String ip,
                                          String data) {
                    elapi.stopEasyLink();

                    if (!"".equals(data)) {
                        JSONObject jsonObj;
                        try {
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

                            easyLinkCallbackContext.success("{\"ip\":\"" + deviceIP + "\",\"mac:\"" + deviceMac + "\"}");


                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                            easyLinkCallbackContext.error("parse JSON obj error");
                        }
                    } else {
                        Log.e(TAG, "socket data is empty!");
                        easyLinkCallbackContext.error("FTC socket data empty");
                    }
                    //elapi.stopFTC();

                }

                @Override
                public void isSmallMTU(int MTU) {
                }
            });
        } else {
            easyLinkCallbackContext.error("args error");
        }
    }

    private void stop() {
        if (elapi != null) {
            elapi.stopEasyLink();
            elapi.stopFTC();
            elapi = null;
        }
    }

    private void resetEasyLink() {
        stop();
        elapi = new EasyLinkAPI(context);
    }

}
