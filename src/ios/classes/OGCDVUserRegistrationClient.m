//  Copyright © 2016 Onegini. All rights reserved.

#import "OGCDVUserRegistrationClient.h"
#import "OGCDVWebBrowserViewController.h"
#import "OGCDVUserClientHelper.h"
#import "OGCDVConstants.h"

@implementation OGCDVUserRegistrationClient {}

- (void)start:(CDVInvokedUrlCommand*)command
{
  [self.commandDelegate runInBackground:^{
      self.callbackId = command.callbackId;
      NSArray *optionalScopes = nil;
      if (command.arguments.count > 0) {
        NSDictionary *options = command.arguments[0];
        optionalScopes = options[OGCDVPluginKeyScopes];
      }
      [[ONGUserClient sharedInstance] registerUser:optionalScopes delegate:self];
  }];
}

- (void)createPin:(CDVInvokedUrlCommand*)command
{
  [self.commandDelegate runInBackground:^{
    self.callbackId = command.callbackId;
    NSDictionary *options = command.arguments[0];
    NSString *pin = options[OGCDVPluginKeyPin];

    if (self.createPinChallenge) {
      [self.createPinChallenge.sender respondWithCreatedPin:pin challenge:self.createPinChallenge];
    } else {
      [self sendErrorResultForCallbackId:command.callbackId withMessage:@"Onegini: createPin called, but no registration in progress. Did you call 'onegini.user.register.start'?"];
    }
  }];
}

- (void)getUserProfiles:(CDVInvokedUrlCommand*)command
{
  NSArray<ONGUserProfile *> *profiles = [[ONGUserClient sharedInstance] userProfiles].allObjects;

  NSMutableArray *result = [[NSMutableArray alloc] initWithCapacity:profiles.count];
  for (ONGUserProfile *profile in profiles) {
    [result addObject:@{OGCDVPluginKeyProfileId: profile.profileId}];
  }

  [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result] callbackId:command.callbackId];
}

- (void)isUserRegistered:(CDVInvokedUrlCommand*)command
{
  NSDictionary *options = command.arguments[0];
  NSString *profileId = options[OGCDVPluginKeyProfileId];

  BOOL isRegistered = [OGCDVUserClientHelper getRegisteredUserProfile:profileId] != nil;

  [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isRegistered] callbackId:command.callbackId];
}

#pragma mark - ONGRegistrationDelegate

- (void)userClient:(ONGUserClient *)userClient didReceivePinRegistrationChallenge:(ONGCreatePinChallenge *)challenge
{
  if (challenge.error != nil) {
    [self sendErrorResultForCallbackId:self.callbackId withError:challenge.error];
    return;
  }

  self.createPinChallenge = challenge;
  [self.viewController dismissViewControllerAnimated:YES completion:nil];

  NSDictionary *result = @{OGCDVPluginKeyPinLength:@(challenge.pinLength)};
  [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result] callbackId:self.callbackId];
}

- (void)userClient:(ONGUserClient *)userClient didReceiveRegistrationRequestWithUrl:(NSURL *)url
{
  // run on the main thread; we initiated registration in a background thread, but now we need to manipulate the UI
  dispatch_async(dispatch_get_main_queue(), ^{
      OGCDVWebBrowserViewController *webBrowserViewController = [OGCDVWebBrowserViewController new];
      webBrowserViewController.url = url;
      webBrowserViewController.completionBlock = ^(NSURL *completionURL) {};
      [self.viewController presentViewController:webBrowserViewController animated:YES completion:nil];
  });
}

- (void)userClient:(ONGUserClient *)userClient didRegisterUser:(ONGUserProfile *)userProfile
{
  self.createPinChallenge = nil;
  NSDictionary *result = @{OGCDVPluginKeyProfileId: userProfile.profileId};
  [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result] callbackId:self.callbackId];
}

- (void)userClient:(ONGUserClient *)userClient didFailToRegisterWithError:(NSError *)error
{
  [self.viewController dismissViewControllerAnimated:YES completion:nil];
  [self sendErrorResultForCallbackId:self.callbackId withError:error];
}

@end