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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Exception;
import java.net.Socket;

/**
 * This class starts transmit to activation
 */
public class mxsdkwrapper extends CordovaPlugin {

    private static String TAG = "=====mxsdkwrapper.class====";
    private CallbackContext easyLinkCallbackContext;
    private Context context;
    //    private FTC_Service ftcService;
    private String mac;
    private String deviceIP;
    private String uid;
    private String token;
    private String APPId;
    private String productKey;
    private String deviceLoginID;
    private String devicePassword;
    //  private String appSecretKey;
    //  private int easylinkVersion;
    private int activateTimeout;
    private String activatePort;
    private Socket socket = null;


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
            uid = args.getString(2);
            APPId = args.getString(3);
            productKey = args.getString(4);
            token = args.getString(5);
            deviceLoginID = args.getString(6);
            devicePassword = args.getString(7);

            if (wifiSSID == null || wifiSSID.length() == 0 ||
                    wifiKey == null || wifiKey.length() == 0 ||
                    uid == null || uid.length() == 0 ||
                    APPId == null || APPId.length() == 0 ||
                    productKey == null || productKey.length() == 0 ||
                    token == null || token.length() == 0 ||
                    devicePassword == null || devicePassword.length() == 0 ||
                    deviceLoginID == null || deviceLoginID.length() == 0
                    ) {
                Log.e(TAG, "arguments error ===== empty");
                return false;
            }

            // todo: replace with EasylinkAPI
            //ftcService = new FTC_Service();
            easyLinkCallbackContext = callbackContext;
            //ftcListener = new FTCLisenerExtension(callbackContext);
            this.transmitSettings(wifiSSID, wifiKey);
            return true;
        }
        if (action.equals("sendDidVerification")) {
            String did = args.getString(0);
            easyLinkCallbackContext = callbackContext;
            sendDidVerification(did);
            return true;
        }
        if (action.equals("dealloc")) {
            final EasyLinkAPI elapi = new EasyLinkAPI(context);
            try {
                elapi.stopEasyLink();
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
        int mobileIp = getMobileIP();
        Log.i(TAG, String.valueOf(mobileIp));
        if (wifiSSID != null && wifiSSID.length() > 0 && wifiKey != null && wifiKey.length() > 0 && mobileIp != 0) {
            final EasyLinkAPI elapi = new EasyLinkAPI(context);
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
                            deviceIP = jsonObj.getJSONArray("C")
                                    .getJSONObject(1).getJSONArray("C")
                                    .getJSONObject(3).getString("C");
                            Log.i(TAG, "findedDeviceIP:" + deviceIP);
                            final String deviceMac = "C89346"
                                    + deviceName.substring(
                                    deviceName.indexOf("(") + 1,
                                    deviceName.length() - 1);

                            //Call Step 2.2
                            //setDevicePwd(socket, deviceLoginID);
//                            final String activeToken = markMd5(deviceMac + userName + devicePassword);
                            //Call Step 3,4,5.
//                            Log.d(TAG, String.valueOf(20));

                            // we need to check the module port has started yet,
                            // may cause the problem that it is always running
                            // to fix it, introduce a timeoutValue to 240 seconds
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    boolean isReady = false;
                                    int timeoutValue = 30;
                                    while (!isReady || !(timeoutValue == 0)) {
                                        try {
                                            Thread.sleep(1000L);
                                            timeoutValue--;
                                        } catch (InterruptedException e) {
                                            Log.e(TAG, e.getMessage());
                                        }

                                        try {

//                                            client = new Socket(deviceIP, 8000);
//                                            client.close();
//                                            client = null;
//                                            isReady = true;
                                            try {
                                                while (!isReady) {
                                                    socket = new Socket(deviceIP, 8000);
                                                    isReady = true;
                                                }
                                            } catch (Exception se) {
                                                Log.e(TAG, se.toString());
                                            }
                                            //socket = new Socket("192.168.10.63", port);
                                            if (isReady) {
                                                final OutputStream os = socket.getOutputStream();
                                                String cmd = "{" + "\"app_id\":\"" + APPId + "\"," +
                                                        "\"product_key\":\"" + productKey + "\"," +
                                                        "\"user_token\":\"" + token + "\"," +
                                                        "\"uid\":\"" + uid +
                                                        "\"}";
                                                os.write(cmd.getBytes());
                                                Log.i(TAG, cmd);

                                                InputStream is = socket.getInputStream();
                                                byte[] reply = new byte[0];
                                                try {
                                                    reply = readStream(is);
                                                } catch (Exception e) {
                                                    Log.e(TAG, e.toString());
                                                    e.printStackTrace();
                                                }

                                                final String replyMessages = new String(reply);
                                                JSONObject activeJSON = null;
                                                String stringResult = "{\"did\": \"" + replyMessages + "\", \"mac\": \"" + mac + "\"}";
                                                activeJSON = new JSONObject(replyMessages);
                                                easyLinkCallbackContext.success(activeJSON);
                                                if (reply.length > 0) {
                                                    break;
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, e.getMessage());
                                            try {
                                                Thread.sleep(3 * 1000L);
                                                timeoutValue = timeoutValue - 3;
                                            } catch (InterruptedException e1) {

                                                Log.e(TAG, e1.getMessage());

                                            }
                                        }
                                    }

//                                    if (isReady) {
//                                        mac = deviceMac;
//                                        HttpPostData(deviceIP, token);
//                                    } else {
//                                        Log.e(TAG, "activate failed");
//                                        easyLinkCallbackContext.error("JSON obj error");
//                                    }
                                }
                            }).start();


                            //Call Step 6. - pls. DO NOT REMOVE
                            //Authorize(activeToken);
                            //easyLinkCallbackContext.success("{\"ip\": \"" + deviceIP + ", \"user_token\": \"" + activeToken + "\"}");
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
            });
        } else {
            easyLinkCallbackContext.error("args error");
        }
    }


    private void sendDidVerification(String did) {
        try {
            final OutputStream os = socket.getOutputStream();
            String cmd = "{" + "\"device_id\":\"" + did +
                    "\"}";
            os.write(cmd.getBytes());
            easyLinkCallbackContext.success("OK");
            if (socket != null) {
                try {
                    socket.close();
                    Log.i(TAG, "Socket closed.");
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            easyLinkCallbackContext.error("Device activate failed.");
            Log.e(TAG, e.getMessage());
        }
    }

    private static byte[] readStream(InputStream inStream) throws Exception {
        int count = 0;
        while (count == 0) {
            count = inStream.available();
            Log.i(TAG, String.valueOf(count));
        }
        byte[] b = new byte[count];
        inStream.read(b);
        return b;
    }

    /**
     * @return 0 if we don't get the mobile device ip, else the mobile device ip
     */
    private int getMobileIP() {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return wifiInfo.getIpAddress();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return 0;
        }
    }

}
