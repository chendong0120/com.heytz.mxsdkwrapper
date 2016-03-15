var exec = require('cordova/exec');

exports.setDeviceWifi = function (wifiSSID,
                                  wifiKey,
                                  success, error) {
    exec(success, error, "mxsdkwrapper", "setDeviceWifi",
        [
            wifiSSID,
            wifiKey,
            moduleDefaultUser,
            moduleDefaultPass
        ]);
};
exports.dealloc = function () {
    exec( null,null,"mxsdkwrapper", "dealloc",
        []);
};
