/********* mxsdkwrapper.m Cordova Plugin Implementation *******/
#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "EasyLink.h"
#import <netinet/in.h>

#define searchingString @"Searching for MXCHIP Modules..."
#define kWebServiceType @"_easylink._tcp"
#define kInitialDomain  @"local"
#define repeatInterval  10.0
bool newModuleFound;
bool enumerating = NO;

@interface mxsdkwrapper : CDVPlugin <NSNetServiceBrowserDelegate,
        NSNetServiceDelegate,EasyLinkFTCDelegate> {
    EASYLINK *easylink_config;
}
@property(nonatomic, retain, readwrite) NSString *easyLinkCallbakId;
@property(nonatomic, retain, readwrite) NSTimer *easyLinkTimer;

- (void)setDeviceWifi:(CDVInvokedUrlCommand *)command;

- (void)dealloc:(CDVInvokedUrlCommand *)command;
@end

@implementation mxsdkwrapper

- (void)pluginInitialize {
}

- (void)setDeviceWifi:(CDVInvokedUrlCommand *)command {
    NSString *wifiSSID = [command.arguments objectAtIndex:0];
    NSString *wifiKey = [command.arguments objectAtIndex:1];
    if (wifiSSID == nil || wifiSSID.length == 0 || wifiKey == nil || wifiKey.length == 0) {
        NSLog(@"Error: arguments");
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    [self stopEasyLink];

    NSMutableDictionary *wlanConfig = [NSMutableDictionary dictionaryWithCapacity:5];
    wlanConfig[KEY_SSID] = [wifiSSID dataUsingEncoding:NSUTF8StringEncoding];
    wlanConfig[KEY_PASSWORD] = wifiKey;
    wlanConfig[KEY_DHCP] = @YES;

    [easylink_config prepareEasyLink_withFTC:wlanConfig info:nil mode:EASYLINK_V2_PLUS];
    [easylink_config transmitSettings];
    _easyLinkCallbakId = command.callbackId;
}

- (void)dealloc:(CDVInvokedUrlCommand *)command {
    [self stopEasyLink];
}

- (void)stopEasyLink {
    NSLog(@"//====dealloc...====");
    if (easylink_config != nil) {
        [easylink_config stopTransmitting];
        [easylink_config unInit];
        easylink_config = nil;
        easylink_config = [[EASYLINK alloc] initForDebug:true WithDelegate:self];
        NSLog(@"Stop EasyLink ali sending.");
    } else {
        easylink_config = [[EASYLINK alloc] initForDebug:true WithDelegate:self];
    }
}

#pragma mark - EasyLink delegate -

- (void)onFound:(NSNumber *)client withName:(NSString *)name mataData:(NSDictionary *)mataDataDict {
    NSMutableDictionary *foundModule = [NSMutableDictionary dictionaryWithDictionary:mataDataDict];
    [foundModule setValue:name forKey:@"N"];
    foundModule[@"FTC"] = @NO;

    NSData *tempData;
    /*
    foundModule:{
        FTC = F;
        FW = "MICO_MQTT_Client_1";
        HD = 3080B;
        ID = 5c33126a;
        MAC = "B0:F8:93:10:09:98";
        MD = EMW3080B;
        MF = "MXCHIP Inc.";
        N = "EMW3080B(100998)";
        OS = "3080B002.004";
        PO = "com.mxchip.mqtt";
        RF = "3080B-3.6a";
        "wlan unconfigured" = F;
    }
    */
    for (NSString *key in mataDataDict) {
        tempData = mataDataDict[key];
        if (tempData != nil) [foundModule setValue:[[NSString alloc] initWithData:tempData encoding:NSUTF8StringEncoding] forKey:key];
    }
    NSMutableDictionary *device = [@{@"name": name, @"mac": @""} mutableCopy];
    device[@"mac"] = [[foundModule objectForKey:@"MAC"] stringByReplacingOccurrencesOfString:@":" withString:@""];
    device[@"name"] = name;
    /*Config is success*/
    NSLog(@"Found by mDNS, client:%d, config success! \n foundModule:%@", [client intValue], [foundModule description]);
    [self stopEasyLink];
    if (_easyLinkCallbakId)
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:device] callbackId:_easyLinkCallbakId];
}

- (void)onFoundByFTC:(NSNumber *)ftcClientTag withConfiguration:(NSDictionary *)configDict {
    /*Config is not success, need to write config to client to finish*/
    NSLog(@"Found by FTC, client:%d", [ftcClientTag intValue]);
     CDVPluginResult *pluginResult = nil;
    @try{
        [easylink_config configFTCClient:ftcClientTag withConfiguration:configDict];
        NSString *deviceName = [configDict objectForKey:@"N"];
        NSLog(@"device name: %@", deviceName);
        NSString * bssidPrefix = @"C89346";
        NSString * deviceNameSplit = [deviceName componentsSeparatedByString:@"("][1];
        NSString  *mac = [bssidPrefix stringByAppendingString:[deviceNameSplit componentsSeparatedByString:@")"][0]];
        NSString *deviceIp = [[[configDict objectForKey:@"C"][1] objectForKey:@"C"][3] objectForKey:@"C"];
        NSDictionary *ret = [NSDictionary dictionaryWithObjectsAndKeys:
                             mac, @"mac",
                             deviceIp, @"ip",
                             nil];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:ret];
    }@catch (NSException *e){
        NSLog(@"error - save configuration..." );
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }@finally{
        if(pluginResult&&_easyLinkCallbakId){
            [self.commandDelegate sendPluginResult:pluginResult callbackId:_easyLinkCallbakId];
        }
        [self stopEasyLink];

    }
}

- (void)onDisconnectFromFTC:(NSNumber *)client withError:(bool)err; {
    if (err == NO){
        NSLog(@"Device disconnected, config success!");
    }else {
        NSLog(@"Device disconnected with error, config failed!");
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"config failed"] callbackId:_easyLinkCallbakId];
    }
    [self stopEasyLink];
}
@end
