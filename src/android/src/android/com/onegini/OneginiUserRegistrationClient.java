package com.onegini;

import static com.onegini.OneginiCordovaPluginConstants.ERROR_CREATE_PIN_NO_REGISTRATION_IN_PROGRESS;
import static com.onegini.OneginiCordovaPluginConstants.ERROR_PLUGIN_INTERNAL_ERROR;
import static com.onegini.OneginiCordovaPluginConstants.PARAM_SCOPES;

import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import com.onegini.handler.CreatePinRequestHandler;
import com.onegini.handler.RegistrationHandler;
import com.onegini.mobile.sdk.android.client.OneginiClient;
import com.onegini.mobile.sdk.android.handlers.request.callback.OneginiPinCallback;
import com.onegini.mobile.sdk.android.model.entity.UserProfile;
import com.onegini.util.PluginResultBuilder;
import com.onegini.util.UserProfileUtil;

public class OneginiUserRegistrationClient extends CordovaPlugin {

  private static final String ACTION_START = "start";
  private static final String ACTION_CREATE_PIN = "createPin";
  private static final String ACTION_GET_USER_PROFILES = "getUserProfiles";

  private RegistrationHandler registrationHandler;

  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (ACTION_START.equals(action)) {
      startRegistration(args, callbackContext);
      return true;
    } else if (ACTION_CREATE_PIN.equals(action)) {
      createPin(args, callbackContext);
      return true;
    } else if (ACTION_GET_USER_PROFILES.equals(action)) {
      getUserProfiles(callbackContext);
      return true;
    }

    return false;
  }

  private void startRegistration(final JSONArray args, final CallbackContext startRegistrationCallbackContext) throws JSONException {
    final String[] scopes;
    JSONArray scopesJSON;

    try {
      scopesJSON = args.getJSONObject(0).getJSONArray(PARAM_SCOPES);
    } catch (JSONException e) {
      scopesJSON = new JSONArray();
    }

    scopes = new String[scopesJSON.length()];
    for (int i = 0; i < scopesJSON.length(); i++) {
      scopes[i] = scopesJSON.getString(i);
    }

    CreatePinRequestHandler.getInstance().setRegistrationCallbackContext(startRegistrationCallbackContext);
    registrationHandler = new RegistrationHandler(startRegistrationCallbackContext);

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        getOneginiClient().getUserClient()
            .registerUser(scopes, registrationHandler);
      }
    });
  }

  private void createPin(final JSONArray args, final CallbackContext createPinCallbackContext) throws JSONException {
    final String pin = args.getJSONObject(0).getString("pin");
    OneginiPinCallback pinCallback = CreatePinRequestHandler.getInstance().getPinCallback();
    CreatePinRequestHandler.getInstance().setCreatePinCallback(createPinCallbackContext);

    if (pinCallback == null) {
      final PluginResult pluginResult = new PluginResultBuilder()
          .withErrorDescription(ERROR_CREATE_PIN_NO_REGISTRATION_IN_PROGRESS)
          .build();
      createPinCallbackContext.sendPluginResult(pluginResult);
    } else {
      registrationHandler.setCallbackContext(createPinCallbackContext);
      pinCallback.acceptAuthenticationRequest(pin.toCharArray());
    }
  }

  public void getUserProfiles(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        final Set<UserProfile> userProfiles = getOneginiClient().getUserClient().getUserProfiles();
        final JSONArray resultPayload;

        try {
          resultPayload = UserProfileUtil.ProfileSetToJSONArray(userProfiles);
        } catch (JSONException e) {
          callbackContext.error(ERROR_PLUGIN_INTERNAL_ERROR + " : " + e.getMessage());
          return;
        }

        callbackContext.success(resultPayload);
      }
    });
  }

  private OneginiClient getOneginiClient() {
    return OneginiSDK.getOneginiClient(cordova.getActivity().getApplicationContext());
  }
}
