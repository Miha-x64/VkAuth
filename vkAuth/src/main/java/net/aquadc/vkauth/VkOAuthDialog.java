package net.aquadc.vkauth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

import static net.aquadc.vkauth.Util.explodeQueryString;

/**
 * Created by mike on 21.02.17
 */

public final class VkOAuthDialog extends DialogFragment {

    private static final String VK_EXTRA_CLIENT_ID = "client_id";
    private static final String VK_EXTRA_SCOPE = "scope";
    private static final String VK_EXTRA_API_VERSION = "version";
    private static final String VK_EXTRA_REVOKE = "revoke";

    private static final String VK_RESULT_INTENT_NAME = "com.vk.auth-token";
    private static final String VK_EXTRA_TOKEN_DATA = "extra-token-data";

    private static final String REDIRECT_URL = "https://oauth.vk.com/blank.html";
    private static final String ERROR = "error";
    private static final String CANCEL = "cancel";

    private ViewGroup view;
    /*pkg*/ View progress;
    private WebView webView;

    private int resultCode = Activity.RESULT_CANCELED;
    private Intent data;
    /*pkg*/ volatile String email;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        this.view = new FrameLayout(activity);
        view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int tenDp = dpToPx(10);
        view.setPadding(tenDp, tenDp, tenDp, tenDp);

        this.progress = new ProgressBar(activity);
        progress.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        view.addView(progress);

        webView = new WebView(activity);
        webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setVisibility(View.INVISIBLE);
        view.addView(webView);

        final Dialog dialog = new Dialog(activity, R.style.VKAlertDialog);
        dialog.setContentView(view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        loadPage();
        if (savedInstanceState != null) {
            email = savedInstanceState.getString("email");
        }
        return dialog;
    }

    @SuppressLint("SetJavaScriptEnabled")
    /*pkg*/ void loadPage() {
        try {
            Bundle parameters = getArguments();
            int appId = parameters.getInt(VK_EXTRA_CLIENT_ID, 0);
            String scope = parameters.getString(VK_EXTRA_SCOPE);
            String apiVersion = parameters.getString(VK_EXTRA_API_VERSION);
            boolean revoke = parameters.getBoolean(VK_EXTRA_REVOKE, false);
            String urlToLoad = String.format(Locale.US,
                    "https://oauth.vk.com/authorize?client_id=%s" +
                            "&scope=%s" +
                            "&redirect_uri=%s" +
                            "&display=mobile" +
                            "&v=%s" +
                            "&response_type=token&revoke=%d",
                    appId, URLEncoder.encode(scope, "UTF-8"), URLEncoder.encode(REDIRECT_URL, "UTF-8"), apiVersion, revoke ? 1 : 0);

            webView.setWebViewClient(new OAuthWebViewClient(this));
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);

            // spy for email, part 1
            if (Build.VERSION.SDK_INT >= 19) {
                webView.addJavascriptInterface(new Object() {
                    @JavascriptInterface
                    public void setEmail(String email) {
                        VkOAuthDialog.this.email = email;
                    }
                }, "SDK");
            }

            webView.loadUrl(urlToLoad);
            webView.setBackgroundColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
            }
            webView.setVerticalScrollBarEnabled(false);
            webView.setVisibility(View.INVISIBLE);
            webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
            progress.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            setCanceledResultAndFinish();
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("email", email);
    }

    private static class OAuthWebViewClient extends WebViewClient {
        boolean canShowPage = true;
        final VkOAuthDialog vkOpenAuthDialog;

        /*pkg*/ OAuthWebViewClient(VkOAuthDialog vkOpenAuthDialog) {
            this.vkOpenAuthDialog = vkOpenAuthDialog;
        }

        boolean processUrl(String url) {
            if (url.startsWith(REDIRECT_URL)) {
                Intent data = new Intent(VK_RESULT_INTENT_NAME);
                String extraData = url.substring(url.indexOf('#') + 1);
                data.putExtra(VK_EXTRA_TOKEN_DATA, extraData);
                Map<String, String> resultParams = explodeQueryString(extraData);

                if (resultParams != null && (resultParams.containsKey(ERROR) || resultParams.containsKey(CANCEL))) {
                    vkOpenAuthDialog.setResultAndFinish(Activity.RESULT_CANCELED, data);
                } else {
                    vkOpenAuthDialog.setResultAndFinish(Activity.RESULT_OK, data);
                }
                return true;
            }
            return false;
        }

        @Override @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (processUrl(url))
                return true;
            canShowPage = true;
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            processUrl(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (canShowPage) {
                vkOpenAuthDialog.progress.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);

                // spy for email, part 2
                if (Build.VERSION.SDK_INT >= 19) {
                    String email = vkOpenAuthDialog.email;
                    if (email != null) {
                        view.evaluateJavascript("document.forms[0].email.value = \"" + email.replace("\\", "\\\\").replace("\"", "\\\"") + "\"", null);
                    }
                    view.evaluateJavascript("document.forms[0].email.onkeyup = function() { SDK.setEmail(document.forms[0].email.value) }", null);
                }
            }
        }

        @Override @SuppressWarnings("deprecation")
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            canShowPage = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext())
                    .setMessage(description)
                    .setPositiveButton(R.string.vk_retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            vkOpenAuthDialog.loadPage();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            vkOpenAuthDialog.dismiss();
                        }
                    });

            builder.show(); // was in try-catch O_o

        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        WaitingForResult receiver = (WaitingForResult) getTargetFragment();
        if (receiver == null) {
            receiver = (WaitingForResult) getActivity();
        }
        if (receiver != null) {
            receiver.onActivityResult((Integer) getArguments().get("request code"), resultCode, data);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        WaitingForResult receiver = (WaitingForResult) getTargetFragment();
        if (receiver == null) {
            receiver = (WaitingForResult) getActivity();
        }
        if (receiver != null) {
            receiver.onActivityResult((Integer) getArguments().get("request code"), resultCode, data);
        }
    }

    private void setCanceledResultAndFinish() {
        resultCode = Activity.RESULT_CANCELED;
        dismiss();
    }

    /*pkg*/ void setResultAndFinish(int code, Intent data) {
        resultCode = code;
        this.data = data;
        dismiss();
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}