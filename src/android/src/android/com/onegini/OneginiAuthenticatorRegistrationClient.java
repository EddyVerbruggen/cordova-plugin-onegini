package com.onegini;

import static com.onegini.OneginiCordovaPluginConstants.ERROR_NO_SUCH_AUTHENTICATOR;
import static com.onegini.OneginiCordovaPluginConstants.ERROR_NO_USER_AUTHENTICATED;

import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import com.onegini.handler.AuthenticatorRegistrationHandler;
import com.onegini.handler.PinAuthenticationRequestHandler;
import com.onegini.mobile.sdk.android.handlers.request.callback.OneginiPinCallback;
import com.onegini.mobile.sdk.android.model.OneginiAuthenticator;
import com.onegini.mobile.sdk.android.model.entity.UserProfile;
import com.onegini.util.ActionArgumentsUtil;
import com.onegini.util.PluginResultBuilder;

public class OneginiAuthenticatorRegistrationClient extends CordovaPlugin {
  private static final String ACTION_REGISTER_NEW = "registerNew";
  private static final String ACTION_PROVIDE_PIN = "providePin";

  private AuthenticatorRegistrationHandler authenticatorRegistrationHandler;

  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (ACTION_REGISTER_NEW.equals(action)) {
      registerNew(args, callbackContext);
      return true;
    } else if (ACTION_PROVIDE_PIN.equals(action)) {
      providePin(args, callbackContext);
      return true;
    }
    return false;
  }

  private void registerNew(final JSONArray args, final CallbackContext registerNewCallbackContext) throws JSONException {
    final UserProfile userProfile = getOneginiClient().getUserClient().getAuthenticatedUserProfile();
    final Set<OneginiAuthenticator> authenticatorSet;
    final OneginiAuthenticator authenticator;

    if (userProfile == null) {
      registerNewCallbackContext.sendPluginResult(new PluginResultBuilder()
          .withErrorDescription(ERROR_NO_USER_AUTHENTICATED)
          .build());

      return;
    }

    authenticatorSet = getOneginiClient().getUserClient().getNotRegisteredAuthenticators(userProfile);
    authenticator = ActionArgumentsUtil.getAuthenticatorFromArguments(args, authenticatorSet);

    if (authenticator == null) {
      registerNewCallbackContext.sendPluginResult(new PluginResultBuilder()
          .withErrorDescription(ERROR_NO_SUCH_AUTHENTICATOR)
          .build());

      return;
    }

    PinAuthenticationRequestHandler.getInstance().setStartAuthenticationCallback(registerNewCallbackContext);
    authenticatorRegistrationHandler = new AuthenticatorRegistrationHandler(registerNewCallbackContext);
    getOneginiClient().getUserClient().registerAuthenticator(authenticator, authenticatorRegistrationHandler);
  }

  private void providePin(final JSONArray args, final CallbackContext providePinCallbackContext) throws JSONException {
    final String pin = ActionArgumentsUtil.getPinFromArguments(args);
    final OneginiPinCallback pinCallback = PinAuthenticationRequestHandler.getInstance().getPinCallback();
    authenticatorRegistrationHandler.setCallbackContext(providePinCallbackContext);

    if (pinCallback == null) {
      providePinCallbackContext.sendPluginResult(new PluginResultBuilder()
          .withErrorDescription(OneginiCordovaPluginConstants.ERROR_PROVIDE_PIN_NO_AUTHENTICATION_IN_PROGRESS)
          .build());
    } else {
      PinAuthenticationRequestHandler.getInstance().setOnNextAuthenticationAttemptCallback(providePinCallbackContext);
      pinCallback.acceptAuthenticationRequest(pin.toCharArray());
    }
  }

  private com.onegini.mobile.sdk.android.client.OneginiClient getOneginiClient() {
    return OneginiSDK.getInstance().getOneginiClient(cordova.getActivity().getApplicationContext());
  }
}