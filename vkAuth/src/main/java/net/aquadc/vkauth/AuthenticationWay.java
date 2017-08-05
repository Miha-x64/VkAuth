package net.aquadc.vkauth;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

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

        @Override <T extends Activity & VkAuthCallbackProvider> void perform(T caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            if (!isAvailable(caller)) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createIntent(extras), RcVkAuth);
        }
        @Override void perform(AppCompatActivity caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            if (!isAvailable(caller)) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createIntent(extras), RcVkAuth);
        }
        @Override void perform(android.app.Fragment caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            if (!isAvailable(caller.getActivity())) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createIntent(extras), RcVkAuth);
        }
        @Override void perform(android.support.v4.app.Fragment caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            if (!isAvailable(caller.getActivity())) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createIntent(extras), RcVkAuth);
        }

        private Intent createIntent(Bundle extras) {
            Intent intent = new Intent(VkAppAuthAction, null);
            intent.setPackage(VkAppPackageId);
            intent.putExtras(extras);
            return intent;
        }
    },
    WebView {
        @Override <T extends Activity & VkAuthCallbackProvider> void perform(T caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            android.app.DialogFragment dialog = new VkOAuthDialogHolder.NativeFragment();
            dialog.setArguments(augumented(extras));
            dialog.show(fragmentManager, null);
        }
        @Override void perform(AppCompatActivity caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            android.support.v4.app.DialogFragment dialog = new VkOAuthDialogHolder.CompatFragment();
            dialog.setArguments(augumented(extras));
            dialog.show(fragmentManager, null);
        }
        @Override void perform(android.app.Fragment caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            android.app.DialogFragment dialog = new VkOAuthDialogHolder.NativeFragment();
            extras.putInt("request code", RcVkAuth);
            dialog.setArguments(extras);
            // can't use requestCode because supporting activities too
            dialog.setTargetFragment(caller, 0);
            dialog.show(fragmentManager, null);
        }
        @Override void perform(android.support.v4.app.Fragment caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            android.support.v4.app.DialogFragment dialog = new VkOAuthDialogHolder.CompatFragment();
            extras.putInt("request code", RcVkAuth);
            dialog.setArguments(extras);
            // can't use requestCode because supporting activities too
            dialog.setTargetFragment(caller, 0);
            dialog.show(fragmentManager, null);
        }

        private Bundle augumented(Bundle extras) {
            extras.putInt("request code", RcVkAuth);
            return extras;
        }
    },
    Auto {
        @Override <T extends Activity & VkAuthCallbackProvider> void perform(T caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller)) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
        @Override void perform(AppCompatActivity caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller)) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
        @Override void perform(Fragment caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller.getActivity())) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
        @Override void perform(android.support.v4.app.Fragment caller, Bundle extras, FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller.getActivity())) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
    };

    public boolean isAvailable(Context context) {
        return true;
    }
    /*pkg*/ abstract <T extends Activity & VkAuthCallbackProvider> void perform(T caller, Bundle extras, android.app.FragmentManager fragmentManager);
    /*pkg*/ abstract void perform(AppCompatActivity caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager);
    /*pkg*/ abstract void perform(android.app.Fragment caller, Bundle extras, android.app.FragmentManager fragmentManager);
    /*pkg*/ abstract void perform(android.support.v4.app.Fragment caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager);
}
