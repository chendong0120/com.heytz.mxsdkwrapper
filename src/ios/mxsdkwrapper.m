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



}
- (void)setDeviceWifi:(CDVInvokedUrlCommand*)command;
- (void)sendDidVerification:(CDVInvokedUrlCommand*)command;
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
    uid = [command.arguments objectAtIndex:2];
    APPId = [command.arguments objectAtIndex:3];
    productKey = [command.arguments objectAtIndex:4];
    token = [command.arguments objectAtIndex:5];
    deviceLoginId = [command.arguments objectAtIndex:6];
    devicePass = [command.arguments objectAtIndex:7];
    
    if (wifiSSID == nil || wifiSSID.length == 0 || wifiKey == nil || wifiKey.length == 0 || uid == nil || uid.length == 0 ) {
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

- (void) onDisconnectFromFTC:(NSNumber *)client
{
    @try {
        if (deviceIp!=nil) {
            if (deviceIp!=nil) {
                NSString * requestUrl =[[NSString alloc] init];
//                requestUrl = [[[[[requestUrl stringByAppendingString:@"http://"] stringByAppendingString:deviceIp]
//                                stringByAppendingString:@":"]
//                                stringByAppendingString:@"8000"]
//                               stringByAppendingString:@"/"];
                requestUrl = [requestUrl stringByAppendingString:deviceIp];
                NSDictionary *parameters = @{@"app_id":APPId,@"product_key":productKey,@"user_token":token,@"uid":uid};
                
                NSString *para=[[[[[[[[@"{\"app_id\":\"" stringByAppendingString:APPId] stringByAppendingString:@"\",\"product_key\":\""]stringByAppendingString:productKey]stringByAppendingString:@"\",\"user_token\":\""]stringByAppendingString:token] stringByAppendingString:@"\",\"uid\":\""]stringByAppendingString:uid]stringByAppendingString:@"\"}"];
                
                sleep(1);
                
                socket= [[FastSocket alloc] initWithHost:requestUrl andPort:@"8000"];
                [socket connect];
                
                NSData *data = [para dataUsingEncoding:NSUTF8StringEncoding];
                long count = [socket sendBytes:[data bytes] count:[data length]];
                
                char bytes[54];
                [socket receiveBytes:bytes count:54];
                NSString *received = [[NSString alloc] initWithBytes:bytes length:54 encoding:NSUTF8StringEncoding];
                
                NSData *jsonData = [received dataUsingEncoding:NSUTF8StringEncoding];
                NSError *err;
                NSDictionary *ret = [NSJSONSerialization JSONObjectWithData:jsonData
                                                                    options:NSJSONReadingMutableContainers
                                                                      error:&err];
                if(err) {
                    NSLog(@"json解析失败：%@",err);
                    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
                }else{
                    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:ret];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
                }
                
            }
        } else {
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
        }
    }
    @catch (NSException *exception) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
    }
    @finally {
        //
    }
    
    
}


- (void)onFoundByFTC:(NSNumber *)ftcClientTag withConfiguration: (NSDictionary *)configDict;
{
    @try{
        [easylink_config configFTCClient:ftcClientTag withConfiguration:configDict];
        NSString *deviceName = [configDict objectForKey:@"N"];
       
        NSLog(@"device name: %@", deviceName);
        
        NSString * bssidPrefix = @"C89346";
        NSString * deviceNameSplit = [deviceName componentsSeparatedByString:@"("][1];
//        bssid = [bssidPrefix stringByAppendingString:[deviceNameSplit componentsSeparatedByString:@")"][0]];
//        NSLog(@"bssid:%@", bssid);
        deviceIp = [[[configDict objectForKey:@"C"][1] objectForKey:@"C"][3] objectForKey:@"C"];
        NSLog(@"device ip: %@", deviceIp);
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

- (void)dealloc
{
    NSLog(@"//====dealloc...====");
    easylink_config.delegate = nil;
    easylink_config = nil;
}

@end