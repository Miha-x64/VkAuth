package net.aquadc.vkauth;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.aquadc.vkauth.Util.explodeQueryString;

/**
 * Created by mike on 21.02.17
 */

public final class VkApp {

    // const

    private static final String VK_APP_FINGERPRINT = "48761EEF50EE53AFC4CC9C5F10E6BDE7F8F5B82F";
    private static final String VK_APP_PACKAGE_ID = "com.vkontakte.android";
    private static final String VK_APP_AUTH_ACTION = "com.vkontakte.android.action.SDK_AUTH";
    private static final String VK_EXTRA_TOKEN_DATA = "extra-token-data";
    private static final String VK_API_VERSION = "5.62";

    private static final int RC_VK_AUTH = 30_109;

    // static

    private static final SparseArray<VkApp> instances = new SparseArray<>(1);

    public static VkApp getInstance(int appId) {
        if (appId <= 0) throw new IllegalArgumentException("invalid appId: " + appId);

        synchronized (instances) {
            VkApp app = instances.get(appId);
            if (app == null) {
                app = new VkApp(appId);
                instances.put(appId, app);
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

    public void login(WaitingForResult receiver, Set<VkScope> scope) {
        if (receiver == null) throw new NullPointerException("receiver is required");
        if (!(receiver instanceof Activity || receiver instanceof Fragment)) {
            throw new IllegalArgumentException(
                    "receiver is expected to be a subclass of either android.app.Activity" +
                            " or android.app.Fragment, got " +
                            receiver.getClass().getSimpleName());
        }
        if (scope == null) throw new NullPointerException("scope is required");

        Bundle extras = new Bundle(4);
        extras.putString("version", VK_API_VERSION);
        extras.putInt("client_id", appId);
        extras.putBoolean("revoke", true); // don't know why, just like in original SDK
        extras.putString("scope", VkScope.joined(scope));

        Intent intent = new Intent(VK_APP_AUTH_ACTION, null);
        intent.setPackage(VK_APP_PACKAGE_ID);
        Activity activity = receiver instanceof Activity
                ? (Activity) receiver : ((Fragment) receiver).getActivity();
        if (activity.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            // app is not installed
            performWebViewAuth(receiver, extras);
            return;
        }

        try {
            String[] certs = Util.getCertificateFingerprints(activity, VK_APP_PACKAGE_ID);
            if (certs.length != 1 || !VK_APP_FINGERPRINT.equals(certs[0])) {
                // todo complain about wrong VK app
                performWebViewAuth(receiver, extras);
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new AssertionError("we've already ensured activity is resolved");
        }

        intent.putExtras(extras);
        receiver.startActivityForResult(intent, RC_VK_AUTH);
    }

    private void performWebViewAuth(WaitingForResult receiver, Bundle extras) {
        VkOAuthDialog dialog = new VkOAuthDialog();
        extras.putInt("request code", RC_VK_AUTH);
        dialog.setArguments(extras);
        if (receiver instanceof Fragment) {
            dialog.setTargetFragment((Fragment) receiver, 0);
        }
        dialog.show(receiver.getFragmentManager(), null);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data, VkAuthCallback callback) {
        if (callback == null) throw new NullPointerException("callback is required");

        if (requestCode != RC_VK_AUTH) return false;

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
        if (extras.containsKey(VK_EXTRA_TOKEN_DATA)) {
            // answer from WebView
            String tokenInfo = extras.getString(VK_EXTRA_TOKEN_DATA);
            tokenParams = explodeQueryString(tokenInfo);
        } else {
            // answer from VK app
            for (String key : extras.keySet()) {
                tokenParams.put(key, String.valueOf(extras.get(key)));
            }
        }

        VkAccessToken newToken = new VkAccessToken(tokenParams);
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
