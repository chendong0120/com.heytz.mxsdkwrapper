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
            userName = args.getString(2);
            //easylinkVersion = args.getInt(3);
            activateTimeout = args.getInt(4);
            activatePort = args.getString(5);
            deviceLoginID = args.getString(6);
            devicePassword = args.getString(7);

            if (wifiSSID == null || wifiSSID.length() == 0 ||
                    wifiKey == null || wifiKey.length() == 0 ||
                    userName == null || userName.length() == 0 ||
                    activatePort == null || activatePort.length() == 0 ||
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
        return false;
    }

    /**
     * Step1. Call FTC Service to start transmit settings.
     *
     * @param wifiSSID
     * @param wifiKey
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
                    //Log.d("FTCEnd", ip + " " + data);
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

                            //Call Step 2.2
                            //setDevicePwd(socket, deviceLoginID);
                            final String activeToken = markMd5(deviceMac + userName + devicePassword);
                            //Call Step 3,4,5.
                            Log.d(TAG, String.valueOf(activateTimeout));


                            (new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    boolean isReady = false;
                                    while (!isReady) {
                                        Socket client = null;
                                        try {
                                            client = new Socket(deviceIP, Integer.parseInt(activatePort));
                                            client.close();
                                            isReady = true;
                                        } catch (Exception e) {
                                            try {
                                                Thread.sleep(3 * 1000L);
                                            } catch (InterruptedException e1) {
                                                e1.printStackTrace();
                                            }
                                        }
                                    }

                                    if (isReady) {
                                        HttpPostData(deviceIP, activeToken);
                                        String stringResult = "{\"active_token\": \"" + activeToken + "\", \"mac\": \"" + deviceMac + "\"}";
                                        Log.i(TAG, stringResult);
                                        JSONObject activeJSON = null;
                                        try {
                                            activeJSON = new JSONObject(stringResult);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        easyLinkCallbackContext.success(activeJSON);
                                    }
                                }
                            })).start();


                            //Call Step 6. - pls. DO NOT REMOVE
                            //Authorize(activeToken);
                            //easyLinkCallbackContext.success("{\"ip\": \"" + deviceIP + ", \"user_token\": \"" + activeToken + "\"}");

                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                            easyLinkCallbackContext.error("JSON obj error");
                        }
                    } else {
                        Log.e(TAG, "socket data is empty!");
                        // _callbackContext.error("FTC socket data empty");
                    }

                }

                @Override
                public void isSmallMTU(int MTU) {
                }
            });
        } else {
            easyLinkCallbackContext.error("args is empty or null");
        }
    }


    /**
     * Step 3,4,5. Send activate request to module,
     * module sends the request to MXChip cloud and then get back device id and return to app.
     *
     * @param activateDeviceIP    device ip need to-be activated
     * @param activateDeviceToken device token need to-be activated
     */
    private void HttpPostData(String activateDeviceIP, String activateDeviceToken) {
        Log.i(TAG, " Step 3. Send activate request to MXChip model.");

        try {
            HttpClient httpclient = new DefaultHttpClient();
            String ACTIVATE_PORT = activatePort;//"8000";
            String ACTIVATE_URL = "/dev-activate";
            String urlString = "http://" + activateDeviceIP + ":" + ACTIVATE_PORT
                    + ACTIVATE_URL;
            Log.i(TAG, "urlString:" + urlString);
            HttpPost httppost = new HttpPost(urlString);
            httppost.addHeader("Content-Type", "application/json");
            httppost.addHeader("Cache-Control", "no-cache");
            JSONObject obj = new JSONObject();
            obj.put("login_id", deviceLoginID);
            obj.put("dev_passwd", devicePassword);
            obj.put("user_token", activateDeviceToken);
            Log.i(TAG, "" + obj.toString());
            httppost.setEntity(new StringEntity(obj.toString()));
            HttpResponse response;
            response = httpclient.execute(httppost);
            int respCode = response.getStatusLine().getStatusCode();
            Log.i(TAG, "respCode:" + respCode);
            String responsesString = EntityUtils.toString(response.getEntity());
            Log.i(TAG, "responsesString:" + responsesString);
            if (respCode == HttpURLConnection.HTTP_OK) {
                JSONObject jsonObject = new JSONObject(responsesString);
                //Get device ID and save in class variable.
                String deviceID = jsonObject.getString("device_id");
                Log.i(TAG, "deviceID:" + deviceID);

            } else {
                easyLinkCallbackContext.error("Device activate failed.");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
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

    /**
     * MD5 algorithm for plain text
     *
     * @param plainText input string
     * @return plainText after md5
     */
    private String markMd5(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }
}
