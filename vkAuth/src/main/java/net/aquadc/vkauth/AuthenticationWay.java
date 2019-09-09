package net.aquadc.vkauth;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import static net.aquadc.vkauth.VkApp.*;

/**
 * Way of authentication.
 */
public enum AuthenticationWay {
    OfficialVkApp {
        @Override public boolean isAvailable(Context context) {
            return VkApp.isInstalled(context);
        }

        @Override void perform(Activity caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            if (!isAvailable(caller)) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createAuthIntent(extras), RcVkAuth);
        }
        @Override void perform(android.support.v7.app.AppCompatActivity caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            if (!isAvailable(caller)) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createAuthIntent(extras), RcVkAuth);
        }
        @Override void perform(AppCompatActivity caller, Bundle extras, FragmentManager fragmentManager) {
            if (!isAvailable(caller)) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createAuthIntent(extras), RcVkAuth);
        }
        @Override void perform(android.app.Fragment caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            if (!isAvailable(caller.getActivity())) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createAuthIntent(extras), RcVkAuth);
        }
        @Override void perform(android.support.v4.app.Fragment caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            if (!isAvailable(caller.getActivity())) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createAuthIntent(extras), RcVkAuth);
        }
        @Override void perform(androidx.fragment.app.Fragment caller, Bundle extras, FragmentManager fragmentManager) {
            if (!isAvailable(caller.getActivity())) throw new IllegalStateException("Official VK app is unavailable.");
            caller.startActivityForResult(createAuthIntent(extras), RcVkAuth);
        }
    },
    WebView {
        @Override void perform(Activity caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            android.app.DialogFragment dialog = new VkOAuthDialogHolder.NativeFragment();
            dialog.setArguments(augumented(extras));
            dialog.show(fragmentManager, null);
        }
        @Override void perform(android.support.v7.app.AppCompatActivity caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            android.support.v4.app.DialogFragment dialog = new VkOAuthDialogHolder.CompatFragment();
            dialog.setArguments(augumented(extras));
            dialog.show(fragmentManager, null);
        }
        @Override void perform(AppCompatActivity caller, Bundle extras, FragmentManager fragmentManager) {
            androidx.fragment.app.DialogFragment dialog = new VkOAuthDialogHolder.XFragment();
            dialog.setArguments(augumented(extras));
            dialog.show(fragmentManager, null);
        }
        @Override void perform(android.app.Fragment caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            android.app.DialogFragment dialog = new VkOAuthDialogHolder.NativeFragment();
            dialog.setArguments(augumented(extras));
            dialog.setTargetFragment(caller, 0);
            dialog.show(fragmentManager, null);
        }
        @Override void perform(android.support.v4.app.Fragment caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            android.support.v4.app.DialogFragment dialog = new VkOAuthDialogHolder.CompatFragment();
            dialog.setArguments(augumented(extras));
            dialog.setTargetFragment(caller, 0);
            dialog.show(fragmentManager, null);
        }
        @Override void perform(androidx.fragment.app.Fragment caller, Bundle extras, FragmentManager fragmentManager) {
            androidx.fragment.app.DialogFragment dialog = new VkOAuthDialogHolder.XFragment();
            dialog.setArguments(augumented(extras));
            dialog.setTargetFragment(caller, 0);
            dialog.show(fragmentManager, null);
        }

        private Bundle augumented(Bundle extras) {
            extras.putInt("request code", RcVkAuth);
            return extras;
        }
    },
    Auto {
        @Override void perform(Activity caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller)) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
        @Override void perform(android.support.v7.app.AppCompatActivity caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller)) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
        @Override void perform(AppCompatActivity caller, Bundle extras, FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller)) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
        @Override void perform(Fragment caller, Bundle extras, android.app.FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller.getActivity())) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
        @Override void perform(android.support.v4.app.Fragment caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller.getActivity())) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
        @Override void perform(androidx.fragment.app.Fragment caller, Bundle extras, FragmentManager fragmentManager) {
            if (OfficialVkApp.isAvailable(caller.getActivity())) OfficialVkApp.perform(caller, extras, fragmentManager);
            else WebView.perform(caller, extras, fragmentManager);
        }
    };

    public boolean isAvailable(Context context) {
        return true;
    }
    /*pkg*/ abstract void perform(Activity caller, Bundle extras, android.app.FragmentManager fragmentManager);
    /*pkg*/ abstract void perform(android.support.v7.app.AppCompatActivity caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager);
    /*pkg*/ abstract void perform(androidx.appcompat.app.AppCompatActivity caller, Bundle extras, androidx.fragment.app.FragmentManager fragmentManager);
    /*pkg*/ abstract void perform(android.app.Fragment caller, Bundle extras, android.app.FragmentManager fragmentManager);
    /*pkg*/ abstract void perform(android.support.v4.app.Fragment caller, Bundle extras, android.support.v4.app.FragmentManager fragmentManager);
    /*pkg*/ abstract void perform(androidx.fragment.app.Fragment caller, Bundle extras, androidx.fragment.app.FragmentManager fragmentManager);
}
