/********* mxsdkwrapper.m Cordova Plugin Implementation *******/
#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "EASYLINK.h"
#import "NSString+MD5.h"
#import "AFNetworking.h"

@interface mxsdkwrapper : CDVPlugin <EasyLinkFTCDelegate> {
    // Member variables go here.
    EASYLINK *easylink_config;
    NSMutableDictionary *deviceIPConfig;
    NSString *loginID;
    CDVInvokedUrlCommand * commandHolder;
    NSString *deviceIp ;
    NSString *userToken ;
    int acitvateTimeout;
    NSString* activatePort;
    NSString* bssid;
    //
    NSString* deviceLoginId;
    NSString* devicePass;
}
- (void)setDeviceWifi:(CDVInvokedUrlCommand*)command;
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
    loginID = [command.arguments objectAtIndex:2];
    deviceLoginId = [command.arguments objectAtIndex:6];
    devicePass = [command.arguments objectAtIndex:7];
    int easylinkVersion;
    activatePort = [command.arguments objectAtIndex:5];
    
    if ([command.arguments objectAtIndex:3] == nil || [command.arguments objectAtIndex:4] == nil) {
        NSLog(@"Error: arguments easylink_version & timeout");
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }else {
        easylinkVersion = [[command.arguments objectAtIndex:3] intValue];
        acitvateTimeout = [[command.arguments objectAtIndex:4] intValue];
    }

    if( wifiKey == nil || wifiKey.length == 0 ){
        wifiKey=@"";
    }

    if (wifiSSID == nil || wifiSSID.length == 0 || loginID == nil || loginID.length == 0 || activatePort==nil || activatePort.length == 0 || deviceLoginId == nil || deviceLoginId.length == 0
        || devicePass == nil || devicePass.length==0) {
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
    EasyLinkMode mode = (EasyLinkMode)easylinkVersion;
    if (!mode) {
        mode = EASYLINK_V2_PLUS;
    }
    [easylink_config prepareEasyLink_withFTC:wlanConfig info:nil mode:mode];
    [easylink_config transmitSettings];
    commandHolder = command;
    
}

- (void) onDisconnectFromFTC:(NSNumber *)client
{
    @try {
        if (deviceIp!=nil) {
            if (deviceIp!=nil && userToken!=nil) {
                NSString * activateUrl = @"dev-activate";
                NSString * requestUrl =[[NSString alloc] init];
                requestUrl = [[[[[[requestUrl stringByAppendingString:@"http://"] stringByAppendingString:deviceIp]
                                 stringByAppendingString:@":"]
                                stringByAppendingString:activatePort]
                               stringByAppendingString:@"/"]
                              stringByAppendingString:activateUrl];
                NSLog(@"request url: %@", requestUrl);
                NSLog(@"user token: %@", userToken);
                NSLog(@"device user: %@", deviceLoginId);
                NSLog(@"device pass: %@", devicePass);
                
                NSDictionary *parameters = @{@"login_id":deviceLoginId,@"dev_passwd":devicePass,@"user_token":userToken};
                
                sleep(acitvateTimeout);
                
                AFHTTPRequestOperationManager *manager = [AFHTTPRequestOperationManager manager];
                
                NSURLRequest *request = [[AFJSONRequestSerializer serializer] requestWithMethod:@"POST" URLString:requestUrl parameters:parameters error:nil];
                AFHTTPRequestOperation *op = [[AFHTTPRequestOperation alloc] initWithRequest:request];
                op.responseSerializer = [AFJSONResponseSerializer serializer];
                
                [op setCompletionBlockWithSuccess:^(AFHTTPRequestOperation *operation, id responseObject) {
                    NSLog(@"JSON: %@", responseObject);
                    NSDictionary *ret = [NSDictionary dictionaryWithObjectsAndKeys:
                                         userToken,@"active_token",
                                         bssid, @"mac",
                                         nil];
                    
                    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:ret];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
                    
                } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
                    NSLog(@"Error: %@", error);
                    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
                    
                }];
                
                [manager.operationQueue addOperation:op];
                
                NSOperationQueue *operationQueue = manager.operationQueue;
                manager.reachabilityManager = [AFNetworkReachabilityManager managerForDomain:requestUrl];
                [manager.reachabilityManager setReachabilityStatusChangeBlock:^(AFNetworkReachabilityStatus status) {
                    switch (status) {
                        case AFNetworkReachabilityStatusReachableViaWWAN:
                            [operationQueue setSuspended:YES];
                            break;
                        case AFNetworkReachabilityStatusReachableViaWiFi:
                            [operationQueue setSuspended:NO];
                            break;
                        case AFNetworkReachabilityStatusNotReachable:
                            [operationQueue setSuspended:YES];
                            break;
                        default:
                            [operationQueue setSuspended:YES];
                            break;
                    }
                }];
                [manager.reachabilityManager startMonitoring];
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
        bssid = [bssidPrefix stringByAppendingString:[deviceNameSplit componentsSeparatedByString:@")"][0]];
        NSLog(@"bssid:%@", bssid);
        deviceIp = [[[configDict objectForKey:@"C"][1] objectForKey:@"C"][3] objectForKey:@"C"];
        NSLog(@"device ip: %@", deviceIp);
        userToken = [[[bssid stringByAppendingString:loginID] stringByAppendingString:devicePass] markMD5];
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