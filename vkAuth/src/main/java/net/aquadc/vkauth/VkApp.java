package net.aquadc.vkauth;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.of;
import static net.aquadc.vkauth.Util.explodeQueryString;

/**
 * Created by mike on 21.02.17
 * Represents an app registered in VK. An entry point for performing auth.
 */

public final class VkApp {

    // const

    /*pkg*/ static final String VkAppFingerprint = "48761EEF50EE53AFC4CC9C5F10E6BDE7F8F5B82F";
    /*pkg*/ static final String VkAppPackageId = "com.vkontakte.android";
    /*pkg*/ static final String VkAppAuthAction = "com.vkontakte.android.action.SDK_AUTH";
    private static final String VkExtraTokenData = "extra-token-data";
    private static final String VkApiVersion = "5.62";

    /*pkg*/ static final int RcVkAuth = 30_109;

    private static final Set<AuthenticationWay> VkAppInstalled =
            unmodifiableSet(of(AuthenticationWay.OfficialVkApp, AuthenticationWay.WebView, AuthenticationWay.Auto));
    private static final Set<AuthenticationWay> VkAppNotInstalled =
            unmodifiableSet(of(AuthenticationWay.WebView, AuthenticationWay.Auto));

    // static

    private static final SparseArray<VkApp> Instances = new SparseArray<>(1);

    public static VkApp getInstance(int appId) {
        if (appId <= 0) throw new IllegalArgumentException("invalid appId: " + appId);

        synchronized (Instances) {
            VkApp app = Instances.get(appId);
            if (app == null) {
                app = new VkApp(appId);
                Instances.put(appId, app);
            }
            return app;
        }
    }

    // instance

    private final int appId;

    private final Object tokenLock = new Object();
    private VkAccessToken currentToken;

    private VkApp(int appId) {
        this.appId = appId;
    }

    /**
     * Returns a set of available authentication ways.
     * @return returned set may contain {@link AuthenticationWay#OfficialVkApp},
     * and will always contain {@link AuthenticationWay#WebView} and {@link AuthenticationWay#Auto}.
     */
    public Set<AuthenticationWay> getAvailableAuthenticationWays(Context context) {
        if (AuthenticationWay.OfficialVkApp.isAvailable(context)) return VkAppInstalled;
        else return VkAppNotInstalled;
    }

    /**
     * Perform auth from Activity.
     * @param receiver          caller activity
     * @param scope             permissions
     * @param authenticationWay way of authentication: Official App, WebView, or decide automatically
     */
    public /* <T extends Activity & WaitingForResult> won't compile :'( */
    void login(WaitingForResult receiver, Set<VkScope> scope, AuthenticationWay authenticationWay) {
        login((Activity) receiver, receiver, scope, authenticationWay);
    }

    /**
     * Perform auth from a Fragment.
     * @param receiver          caller fragment
     * @param scope             permissions
     * @param authenticationWay way of authentication: Official App, WebView, or decide automatically
     */
    public <T extends Fragment & WaitingForResult> void login(T receiver, Set<VkScope> scope, AuthenticationWay authenticationWay) {
        login(receiver.getActivity(), receiver, scope, authenticationWay);
    }

    private void login(Context context, WaitingForResult receiver, Set<VkScope> scope, AuthenticationWay authenticationWay) {
        if (context == null) throw new NullPointerException("context is required");
        if (receiver == null) throw new NullPointerException("receiver is required");
        if (!(receiver instanceof Activity || receiver instanceof Fragment)) {
            throw new IllegalArgumentException(
                    "receiver is expected to be a subclass of either android.app.Activity" +
                            " or android.app.Fragment, got " +
                            receiver.getClass().getName());
        }
        if (scope == null) throw new NullPointerException("scope is required");
        if (authenticationWay == null) throw new NullPointerException("authenticationWay is required");

        Bundle extras = new Bundle(4);
        extras.putString("version", VkApiVersion);
        extras.putInt("client_id", appId);
        extras.putBoolean("revoke", true); // don't know why, just like in original SDK
        extras.putString("scope", VkScope.joined(scope));

        if (!authenticationWay.isAvailable(context)) {
            throw new IllegalStateException("Authentication way " + authenticationWay + " is unavailable.");
        }
        authenticationWay.perform(context, receiver, extras);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data, VkAuthCallback callback) {
        if (callback == null) throw new NullPointerException("callback is required");

        if (requestCode != RcVkAuth) return false;

        if (resultCode != Activity.RESULT_OK) {
            callback.onError();
            return true;
        }

        if (data == null) {
            callback.onError();
            return true;
        }

        Bundle extras = data.getExtras();
        Map<String, String> tokenParams = new HashMap<>();
        if (extras.containsKey(VkExtraTokenData)) {
            // answer from WebView
            String tokenInfo = extras.getString(VkExtraTokenData);
            tokenParams = explodeQueryString(tokenInfo);
        } else {
            // answer from VK app
            for (String key : extras.keySet()) {
                tokenParams.put(key, String.valueOf(extras.get(key)));
            }
        }

        VkAccessToken newToken = VkAccessToken.create(tokenParams);
        if (newToken == null) {
            callback.onError();
            return false;
        }

        synchronized (tokenLock) {
            if (currentToken == null) {
                currentToken = newToken;
            } else {
                currentToken = newToken = currentToken.overriddenBy(newToken);
            }
        }

        callback.onResult(newToken);
        return true;
    }

    public interface VkAuthCallback {
        void onResult(VkAccessToken token);
        void onError();
    }

}
