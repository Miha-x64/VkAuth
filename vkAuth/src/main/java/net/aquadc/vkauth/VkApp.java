package net.aquadc.vkauth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.of;
import static net.aquadc.vkauth.Util.explodeQueryString;
import static net.aquadc.vkauth.Util.required;

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
     * Perform auth from android.app.Activity through native fragment.
     * @param caller            Activity which implements VkAuthCallbackProvider
     * @param scope             permissions
     * @param authenticationWay a way of authentication
     * @param fragmentManager   a fragment manager
     */
    public <A extends Activity & VkAuthCallbackProvider> void login(A caller, Set<VkScope> scope, AuthenticationWay authenticationWay, android.app.FragmentManager fragmentManager) {
        required(caller, "caller", scope, "scope", authenticationWay, "authenticationWay", fragmentManager, "fragmentManager");
        authenticationWay.perform(caller, createRequestBundle(scope), fragmentManager);
    }

    /**
     * Perform auth from android.support.v7.app.AppCompatActivity through fragment back-port.
     * @param caller            AppCompatActivity which implements VkAuthCallbackProvider
     * @param scope             permissions
     * @param authenticationWay a way of authentication
     * @param fragmentManager   a fragment manager
     */
    public <A extends AppCompatActivity & VkAuthCallbackProvider> void login(A caller, Set<VkScope> scope, AuthenticationWay authenticationWay, android.support.v4.app.FragmentManager fragmentManager) {
        required(caller, "caller", scope, "scope", authenticationWay, "authenticationWay", fragmentManager, "fragmentManager");
        authenticationWay.perform(caller, createRequestBundle(scope), fragmentManager);
    }

    /**
     * Perform auth from android.app.Fragment.
     * @param caller            caller Fragment
     * @param scope             permissions
     * @param authenticationWay a way of authentication
     * @param fragmentManager   a fragment manager
     */
    public void login(android.app.Fragment caller, Set<VkScope> scope, AuthenticationWay authenticationWay, android.app.FragmentManager fragmentManager) {
        required(caller, "caller", caller.getActivity(), "caller.getActivity()", scope, "scope", authenticationWay, "authenticationWay");
        authenticationWay.perform(caller, createRequestBundle(scope), fragmentManager);
    }

    /**
     * Perform auth from android.support.v4.app.Fragment.
     * @param caller            caller Fragment
     * @param scope             permissions
     * @param authenticationWay a way of authentication
     * @param fragmentManager   a fragment manager
     */
    public void login(android.support.v4.app.Fragment caller, Set<VkScope> scope, AuthenticationWay authenticationWay, android.support.v4.app.FragmentManager fragmentManager) {
        required(caller, "caller", caller.getActivity(), "caller.getActivity()", scope, "scope", authenticationWay, "authenticationWay");
        authenticationWay.perform(caller, createRequestBundle(scope), fragmentManager);
    }

    private Bundle createRequestBundle(Set<VkScope> scope) {
        Bundle extras = new Bundle(5);
        extras.putString("version", VkApiVersion);
        extras.putInt("client_id", appId);
        extras.putBoolean("revoke", true); // don't know why, just like in original SDK
        extras.putString("scope", VkScope.joined(scope));
        return extras;
    }

    public <T extends Activity & VkAuthCallbackProvider> boolean onActivityResult(
            T caller, int requestCode, int resultCode, Intent data) {
        VkAuthCallback callback;
        required(caller, "caller", callback = caller.getVkAuthCallback(), "caller.getVkAuthCallback()");

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

    public interface VkAuthCallbackProvider {
        VkAuthCallback getVkAuthCallback();
    }
    public interface VkAuthCallback {
        void onResult(VkAccessToken token);
        void onError();
    }

}
