/********* mxsdkwrapper.m Cordova Plugin Implementation *******/
#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "EASYLINK.h"
#import "AFNetworking.h"
#import "FastSocket.h"

@interface mxsdkwrapper : CDVPlugin <EasyLinkFTCDelegate> {
    // Member variables go here.
    EASYLINK *easylink_config;
    NSMutableDictionary *deviceIPConfig;
    NSString *uid;
    CDVInvokedUrlCommand * commandHolder;
    NSString *deviceIp ;
    NSString *userToken ;
    NSString *APPId ;
    NSString *productKey ;
    NSString *token ;
    NSString *activatePort;
    NSString *device_id;
    NSString *mac ;
    NSString *deviceLoginId;
    NSString *devicePass;
    FastSocket *socket;
    NSThread *threadTCP;
    NSString *para;
    NSString * requestUrl;
    
    
}
- (void)setDeviceWifi:(CDVInvokedUrlCommand*)command;
- (void)sendDidVerification:(CDVInvokedUrlCommand*)command;
- (void)dealloc:(CDVInvokedUrlCommand*)command;
@end

@implementation mxsdkwrapper

-(void)pluginInitialize{
    easylink_config = nil;
}

- (void)setDeviceWifi:(CDVInvokedUrlCommand*)command
{
    if (easylink_config == nil || easylink_config.delegate == nil) {
        easylink_config = [[EASYLINK alloc]initWithDelegate:self];
    }
    NSString* wifiSSID = [command.arguments objectAtIndex:0];
    NSString* wifiKey = [command.arguments objectAtIndex:1];
    
    if(wifiKey == nil || wifiKey.length == 0 ){
        wifiKey = "";
    }
    
    if (wifiSSID == nil || wifiSSID.length == 0 ) {
        NSLog(@"Error: arguments");
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    NSMutableDictionary *wlanConfig = [NSMutableDictionary dictionaryWithCapacity:20];
    [wlanConfig setObject:[wifiSSID dataUsingEncoding:NSUTF8StringEncoding] forKey:KEY_SSID];
    [wlanConfig setObject:wifiKey forKey:KEY_PASSWORD];
    //this should be always YES as currently only support DHCP mode
    [wlanConfig setObject:[NSNumber numberWithBool:@YES] forKey:KEY_DHCP];
    //use default value
    EasyLinkMode mode = (EasyLinkMode)3;
    if (!mode) {
        mode = EASYLINK_V2_PLUS;
    }
    [easylink_config prepareEasyLink_withFTC:wlanConfig info:nil mode:mode];
    [easylink_config transmitSettings];
    commandHolder = command;
    
}

- (void)sendDidVerification:(CDVInvokedUrlCommand*)command
{
    NSString* did = [command.arguments objectAtIndex:0];
    commandHolder = command;
    NSString *para=[[@"{\"device_id\":\"" stringByAppendingString:did]stringByAppendingString:@"\"}"];
    NSData *data = [para dataUsingEncoding:NSUTF8StringEncoding];
    long count = [socket sendBytes:[data bytes] count:[data length]];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"OK"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
    
}

- (void) onDisconnectFromFTC:(NSNumber *)client{}


- (void)onFoundByFTC:(NSNumber *)ftcClientTag withConfiguration: (NSDictionary *)configDict;
{
    @try{
        [easylink_config configFTCClient:ftcClientTag withConfiguration:configDict];
        NSString *deviceName = [configDict objectForKey:@"N"];
        
        NSLog(@"device name: %@", deviceName);
        
        NSString * bssidPrefix = @"C89346";
        NSString * deviceNameSplit = [deviceName componentsSeparatedByString:@"("][1];
        CDVPluginResult *pluginResult = nil;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];

    }
    @catch (NSException *e){
        NSLog(@"error - save configuration..." );
        CDVPluginResult *pluginResult = nil;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
    }
    @finally{
        if (easylink_config !=nil) {
            [easylink_config stopTransmitting];
        }
    }
}

- (void)dealloc:(CDVInvokedUrlCommand*)command
{
    NSLog(@"//====dealloc...====");
    if (easylink_config !=nil) {
        [easylink_config stopTransmitting];
    }
    if(socket!=nil)
    {
        [socket close];
    }
    //    easylink_config.delegate = nil;
    //    easylink_config = nil;
}

@end