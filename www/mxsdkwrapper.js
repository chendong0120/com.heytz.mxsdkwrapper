var exec = require('cordova/exec');

exports.setDeviceWifi = function (wifiSSID, wifiKey, username, success, error) {
    exec(success, error, "mxsdkwrapper", "setDeviceWifi", [wifiSSID, wifiKey, username]);
};
