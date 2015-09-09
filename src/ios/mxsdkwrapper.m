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
    NSString * bssid;
}
- (void)setDeviceWifi:(CDVInvokedUrlCommand*)command;
@end

@implementation mxsdkwrapper

-(void)pluginInitialize{

    easylink_config = [[EASYLINK alloc]initWithDelegate:self];
}

- (void)setDeviceWifi:(CDVInvokedUrlCommand*)command
{
    NSString* wifiSSID = [command.arguments objectAtIndex:0];
    NSString* wifiKey = [command.arguments objectAtIndex:1];
    loginID = [command.arguments objectAtIndex:2];

    if (wifiSSID == nil || wifiSSID.length == 0 || wifiKey == nil || wifiKey.length == 0 || loginID == nil || loginID.length == 0) {
        NSLog(@"Error: arguments");
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }

    NSMutableDictionary *wlanConfig = [NSMutableDictionary dictionaryWithCapacity:20];
    [wlanConfig setObject:[wifiSSID dataUsingEncoding:NSUTF8StringEncoding] forKey:KEY_SSID];
    [wlanConfig setObject:wifiKey forKey:KEY_PASSWORD];
    ////this should be always YES as currently only support DHCP mode
    [wlanConfig setObject:[NSNumber numberWithBool:@YES] forKey:KEY_DHCP];
    //use default value
    [easylink_config prepareEasyLink_withFTC:wlanConfig info:nil mode:EASYLINK_V2_PLUS];
    [easylink_config transmitSettings];
    commandHolder = command;

}

- (void) onDisconnectFromFTC:(NSNumber *)client
{
    if (deviceIp!=nil) {
        if (deviceIp!=nil && userToken!=nil) {

            NSString * activateUrl = @"dev-activate";
            NSString * requestUrl =[[NSString alloc] init];
            requestUrl = [[[[requestUrl stringByAppendingString:@"http://"] stringByAppendingString:deviceIp]
                           stringByAppendingString:@":8000/"] stringByAppendingString:activateUrl];

            NSLog(@"request url: %@", requestUrl);
            NSLog(@"user token: %@", userToken);

            NSDictionary *parameters = @{@"login_id":@"admin",@"dev_passwd":@"admin",@"user_token":userToken};

            sleep(3);

            AFHTTPRequestOperationManager *manager = [AFHTTPRequestOperationManager manager];

            NSURLRequest *request = [[AFJSONRequestSerializer serializer] requestWithMethod:@"POST" URLString:requestUrl parameters:parameters error:nil];
            AFHTTPRequestOperation *op = [[AFHTTPRequestOperation alloc] initWithRequest:request];
            op.responseSerializer = [AFJSONResponseSerializer serializer];

            [op setCompletionBlockWithSuccess:^(AFHTTPRequestOperation *operation, id responseObject) {
                NSLog(@"JSON: %@", responseObject);
                NSDictionary *ret = [NSDictionary dictionaryWithObjectsAndKeys:
                    userToken,@"active_token",
                    bssid,@"mac",
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
        userToken = [[[bssid stringByAppendingString:loginID] stringByAppendingString:@"admin"] markMD5];

    }
    @catch (NSException *e){
        NSLog(@"error - save configuration..." );
        CDVPluginResult *pluginResult = nil;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:commandHolder.callbackId];
    }
    @finally{
        [easylink_config stopTransmitting];
    }
}

- (void)dealloc
{
    easylink_config = nil;
}

@end
