/********* mxsdkwrapper.m Cordova Plugin Implementation *******/
#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "EASYLINK.h"


@interface mxsdkwrapper : CDVPlugin <EasyLinkFTCDelegate> {
    EASYLINK *easylink_config;

}
- (void)setDeviceWifi:(CDVInvokedUrlCommand*)command;
//- (void)sendDidVerification:(CDVInvokedUrlCommand*)command;
- (void)dealloc:(CDVInvokedUrlCommand*)command;
@end

@implementation mxsdkwrapper

-(void)pluginInitialize{
}

- (void)setDeviceWifi:(CDVInvokedUrlCommand*)command
{
    NSString* wifiSSID = [command.arguments objectAtIndex:0];
    NSString* wifiKey = [command.arguments objectAtIndex:1];
    if (wifiSSID == nil || wifiSSID.length == 0 || wifiKey == nil || wifiKey.length == 0 ) {
        NSLog(@"Error: arguments");
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
     [self stopEasyLink ];

    NSMutableDictionary *wlanConfig = [NSMutableDictionary dictionaryWithCapacity:5];

    [wlanConfig setObject:wifiSSID forKey:KEY_SSID];
    [wlanConfig setObject:wifiKey forKey:KEY_PASSWORD];
    [wlanConfig setObject:[NSNumber numberWithBool:YES] forKey:KEY_DHCP];

    [easylink_config prepareEasyLink:wlanConfig
                                info:nil
                                mode:EASYLINK_V2_PLUS
                             encrypt:[@"" dataUsingEncoding:NSUTF8StringEncoding] ];
    [easylink_config transmitSettings];


}

- (void)dealloc:(CDVInvokedUrlCommand*)command
{
    [self stopEasyLink ];
}
-(void)stopEasyLink{
    NSLog(@"//====dealloc...====");
    if (easylink_config !=nil) {
        [easylink_config stopTransmitting];
        [easylink_config unInit];
        easylink_config = nil;
         easylink_config = [[EASYLINK alloc]initForDebug:true WithDelegate:self];
        NSLog(@"Stop EasyLink ali sending.");
    }else{
         easylink_config = [[EASYLINK alloc]initForDebug:true WithDelegate:self];
    }
}

#pragma mark - EasyLink delegate -

- (void)onFound:(NSNumber *)client withName:(NSString *)name mataData: (NSDictionary *)mataDataDict
{
    /*Config is success*/
    NSLog(@"Found by mDNS, client:%d, config success!", [client intValue]);
    [easylink_config stopTransmitting];
}

- (void)onFoundByFTC:(NSNumber *)ftcClientTag withConfiguration: (NSDictionary *)configDict
{
    /*Config is not success, need to write config to client to finish*/
    NSLog(@"Found by FTC, client:%d", [ftcClientTag intValue]);
    [easylink_config configFTCClient:ftcClientTag withConfiguration: [NSDictionary dictionary] ];
    [easylink_config stopTransmitting];
}

- (void)onDisconnectFromFTC:(NSNumber *)client  withError:(bool)err;
{
    if(err == NO)
        NSLog(@"Device disconnected, config success!");
    else
        NSLog(@"Device disconnected with error, config failed!");
}

@end