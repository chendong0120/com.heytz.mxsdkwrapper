//
//  NSString+MD5.h
//  suntastic-demo
//
//  Created by Michael Chai on 15-4-9.
//
//

#import <Foundation/Foundation.h>
#import <CommonCrypto/CommonDigest.h>

@interface NSString (MD5)
-(NSString*) markMD5;
@end
