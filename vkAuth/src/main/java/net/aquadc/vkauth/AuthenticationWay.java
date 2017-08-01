package net.aquadc.vkauth;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import static net.aquadc.vkauth.VkApp.*;

/**
 * Way of authentification.
 */
public enum AuthenticationWay {
    OfficialVkApp {
        @Override public boolean isAvailable(Context context) {
            Intent intent = new Intent(VkAppAuthAction, null);
            intent.setPackage(VkAppPackageId);
            if (context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                return false;
            }

            try {
                String[] certs = Util.getCertificateFingerprints(context, VkAppPackageId);
                if (certs.length != 1 || !VkAppFingerprint.equals(certs[0])) {
                    // todo complain about wrong VK app
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new AssertionError("we've already ensured activity is resolved");
            }

            return true;
        }

        @Override void perform(Context context, WaitingForResult receiver, Bundle extras) {
            Intent intent = new Intent(VkAppAuthAction, null);
            intent.setPackage(VkAppPackageId);
            intent.putExtras(extras);
            receiver.startActivityForResult(intent, RcVkAuth);
        }
    },
    WebView {
        @Override void perform(Context context, WaitingForResult receiver, Bundle extras) {
            VkOAuthDialog dialog = new VkOAuthDialog();
            extras.putInt("request code", RcVkAuth);
            dialog.setArguments(extras);
            if (receiver instanceof Fragment) {
                // can't use requestCode because supporting activities too
                dialog.setTargetFragment((Fragment) receiver, 0);
            }
            dialog.show(receiver.getFragmentManager(), null);
        }
    },
    Auto {
        @Override void perform(Context context, WaitingForResult receiver, Bundle extras) {
            if (OfficialVkApp.isAvailable(context)) OfficialVkApp.perform(context, receiver, extras);
            else WebView.perform(context, receiver, extras);
        }
    };

    public boolean isAvailable(Context context) {
        return true;
    }
    /*pkg*/ abstract void perform(Context context, WaitingForResult receiver, Bundle extras);
}
