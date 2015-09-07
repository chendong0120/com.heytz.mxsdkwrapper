package com.heytz.mxsdkwrapper;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.mxchip.ftc_service.FTC_Listener;
import com.mxchip.ftc_service.FTC_Service;
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
    private FTC_Service ftcService;
    private String appID;
    private String userName;
    private String activeToken;
    private String deviceLoginID;
    private String devicePassword;
    private String deviceID;
    private String appSecretKey;

    /**
     * Step 2.1 FTC Call back, process the response from MXChip Model.
     */
    private FTC_Listener ftcListener = new FTC_Listener() {
        /**
         * ftc callback function - on FTC finished
         * @param socket
         * @param data
         */
        @Override
        public void onFTCfinished(final Socket socket, String data) {
            Log.i(TAG, " Step 2.1 FTC Call back, process the response from MXChip Model.");
            if (null != socket) {
                if (!"".equals(data)) {
                    JSONObject jsonObj;
                    try {
                        jsonObj = new JSONObject(data);

                        String deviceName = jsonObj.getString("N");
                        String deviceIP = jsonObj.getJSONArray("C")
                                .getJSONObject(1).getJSONArray("C")
                                .getJSONObject(3).getString("C");
                        Log.i(TAG, "findedDeviceIP:" + deviceIP);
                        String deviceMac = "C89346"
                                + deviceName.substring(
                                deviceName.indexOf("(") + 1,
                                deviceName.length() - 1);

                        //Call Step 2.2
                        setDevicePwd(socket, deviceLoginID);
                        activeToken = markMd5(deviceMac + userName + devicePassword);
                        //Call Step 3,4,5.
                        try {
                            //Violently waiting for 3 seconds.
                            Thread.sleep(3000);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                        HttpPostData(deviceIP, activeToken);
                        easyLinkCallbackContext.success("{\"active_token\": \"" + activeToken + "\"}");
                        //Call Step 6.
                        //Authorize(activeToken);
                        //easyLinkCallbackContext.success("{\"ip\": \"" + deviceIP + ", \"user_token\": \"" + activeToken + "\"}");

                    } catch (JSONException e) {
                        easyLinkCallbackContext.error("JSON obj error");
                    }
                } else {
                    easyLinkCallbackContext.error("FTC socket data empty");
                }
            } else {
                easyLinkCallbackContext.error("FTC socket Null");
            }
        }
    };

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // your init code here
        //appID = "bf2c0256-431d-460a-af8b-922f76f22941";
        //appSecretKey = "d8f87517cd8b3ee65326961bf14aa8c5";
        deviceLoginID = "admin";
        devicePassword = "admin";
        context = cordova.getActivity().getApplicationContext();

        ftcService = new FTC_Service();

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("transmitSettings")) {
            String wifiSSID = args.getString(0);
            String wifiKey = args.getString(1);
            userName = args.getString(2);
            appID = args.getString(3);
            appSecretKey = args.getString(4);
            if (wifiSSID == null || wifiSSID.length() == 0 ||
                    wifiKey == null || wifiKey.length() == 0 ||
                    userName == null || userName.length() == 0 ||
                    appID == null || appID.length() == 0 ||
                    appSecretKey == null || appSecretKey.length() == 0) {
                return false;
            }
            easyLinkCallbackContext = callbackContext;
            this.transmitSettings(args.getString(0), args.getString(1));
            return true;
        }
        return false;
    }

    /**
     * Step1. Call FTC Service to start transmit settings.
     * @param wifiSSID
     * @param wifiKey
     */
    private void transmitSettings(String wifiSSID, String wifiKey) {
        Log.i(TAG, " Step1. Call FTC Service to transmit settings. SSID = " + wifiSSID + ", Password = " + wifiKey);
        int mobileIp = getMobileIP();
        Log.i(TAG, String.valueOf(mobileIp));
        if (wifiSSID != null && wifiSSID.length() > 0 && wifiKey != null && wifiKey.length() > 0 && mobileIp != 0) {
            ftcService.transmitSettings(wifiSSID, wifiKey, mobileIp, ftcListener);
        } else {
            easyLinkCallbackContext.error("args is empty or null");
        }
    }

    /**
     * Step 2.2 FTC call back function to send pwd via socket. Step 2 finish.
     * 等待200或300成功信息回复（第一次配对发200，第二次发300）,现在不做区分，仅作参考。
     *
     * @param socket
     * @param pwd
     */
    private void setDevicePwd(Socket socket, String pwd) {
        Log.i(TAG, " Step 2.2 FTC call back function to send pwd via socket. Step 2 finish.");
        String configString = "{}";
        OutputStream oStream = null;
        try {
            if (pwd != null && !pwd.equals("")) {
                configString = "{\"devPasswd\": \"" + pwd + "\"}";
            }
            int contentLength = configString.length();
            oStream = socket.getOutputStream();
            String outString = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
                    + contentLength
                    + "\r\nConnection: keep-alive\r\n\r\n"
                    + configString + "";
            oStream.write(outString.getBytes());
            oStream.close();
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            easyLinkCallbackContext.error(e.getMessage());
        } finally {
            if (oStream != null) {
                try {
                    oStream.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
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
            String ACTIVATE_PORT = "8000";
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
                deviceID = jsonObject.getString("device_id");
                Log.i(TAG, "deviceID:" + deviceID);

            } else {
                easyLinkCallbackContext.error("Device activate failed.");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    //Step 6. Do not need to consider the code argument, it is useless now.
    private void Authorize(String authorizeActiveToken) {
        Log.i(TAG, "in Authorize");
        JSONObject postParam = new JSONObject();
        try {

            postParam.put("active_token", authorizeActiveToken);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        //TODO APPID need to dynamically send into wrapper;
        HttpUtils http = new HttpUtils();
        int HTTP_TIMEOUT = 15000;
        http.configTimeout(HTTP_TIMEOUT);
        RequestParams params = new RequestParams();
        params.addHeader("Content-Type", "application/json");
        params.addHeader("Authorization", "token " + activeToken);
        params.addHeader("X-Application-Id", appID);
        params.addHeader("X-Request-Sign", getRequestSign(appSecretKey));

        try {
            params.setBodyEntity(new StringEntity(postParam.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
        }

        http.send(HttpRequest.HttpMethod.POST,
                "http://api.easylink.io/v1/key/authorize.json", params,
                new RequestCallBack<String>() {
                    @Override
                    public void onLoading(long total, long current,
                                          boolean isUploading) {

                    }

                    @Override
                    public void onSuccess(ResponseInfo<String> responseInfo) {
                        try {
                            Log.i(TAG, " in testAauthorize onSuccess" + responseInfo.result.toString());

                            JSONObject jsonObject = new JSONObject(responseInfo.result);
                            String did = jsonObject.getString("user_device");
                            Log.i(TAG, " deviceID:" + did);

                            easyLinkCallbackContext.success("{\"device_id\": \"" + deviceID + "\"}");

                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }

                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onFailure(HttpException error, String msg) {
                        Log.i(TAG, " in Authorize onFailure");
                        // Log.i(TAG, "message===" + msg.toString());
                        // Log.i(TAG, "httperror====" + error.toString());

                        easyLinkCallbackContext.error("mxsdkwrapper setDevicePwd Authorize function Error");
                    }
                });

    }

    /**
     * @return 0 if we don't get the mobile device ip, else the mobile divice ip
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

    /**
     * @param appsecretkey application secret key
     * @return requesting signature
     */
    private String getRequestSign(String appsecretkey) {
        long timetemp = System.currentTimeMillis() / 1000;
        String sign = markMd5(appsecretkey + timetemp);
        Log.i("======getRequestSign==", sign + "=====time" + timetemp);
        return sign + "," + timetemp;
    }
}
