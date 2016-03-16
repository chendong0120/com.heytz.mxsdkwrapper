var exec = require('cordova/exec');

exports.setDeviceWifi = function (wifiSSID,
                                  wifiKey,
                                  success, error) {
    exec(success, error, "mxsdkwrapper", "setDeviceWifi",
        [
            wifiSSID,
            wifiKey
        ]);
};
exports.dealloc = function () {
    exec( null,null,"mxsdkwrapper", "dealloc",
        []);
};
