var exec = require('cordova/exec');

exports.setDeviceWifi = function (wifiSSID,
                                  wifiKey,
                                  username,
                                  easylinkVersion,
                                  activateTimeout,
                                  activatePort,
                                  moduleDefaultUser,
                                  moduleDefaultPass,
                                  success, error) {
    exec(success, error, "mxsdkwrapper", "setDeviceWifi",
        [
            wifiSSID,
            wifiKey,
            username,
            easylinkVersion,
            activateTimeout,
            activatePort,
            moduleDefaultUser,
            moduleDefaultPass
        ]);
};
exports.sendDidVerification = function (did,
                                        success, error) {
    exec(success, error, "mxsdkwrapper", "sendDidVerification",
        [
            did
        ]);
};
exports.dealloc = function () {
    exec( "mxsdkwrapper", "sendDidVerification",
        []);
};

