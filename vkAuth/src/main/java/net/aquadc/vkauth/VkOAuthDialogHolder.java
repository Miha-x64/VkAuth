package net.aquadc.vkauth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

/*pkg*/ final class VkOAuthDialogHolder {

    private static final String VK_EXTRA_CLIENT_ID = "client_id";
    private static final String VK_EXTRA_SCOPE = "scope";
    private static final String VK_EXTRA_API_VERSION = "version";
    private static final String VK_EXTRA_REVOKE = "revoke";

    private static final String VK_RESULT_INTENT_NAME = "com.vk.auth-token";
    private static final String VK_EXTRA_TOKEN_DATA = "extra-token-data";

    private static final String REDIRECT_URL = "https://oauth.vk.com/blank.html";
    private static final String ERROR = "error";
    private static final String CANCEL = "deliverResult";

    /*pkg*/ final ViewGroup root;
    /*pkg*/ final View progress;
    /*pkg*/ final WebView webView;
    /*pkg*/ final Dialog dialog;
    /*pkg*/ final Bundle arguments;
    private final Host host;

    /*pkg*/ volatile String email;
    private int resultCode = Activity.RESULT_CANCELED;
    private Intent data;

    /*pkg*/ VkOAuthDialogHolder(Context context, Bundle arguments, Bundle savedInstanceState, Host host) {
        this.root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int tenDp = dp(10);
        root.setPadding(tenDp, tenDp, tenDp, tenDp);

        this.progress = new ProgressBar(context);
        progress.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        root.addView(progress);

        webView = new WebView(context);
        webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setVisibility(View.INVISIBLE);
        root.addView(webView);

        dialog = new Dialog(context, R.style.VKAlertDialog);
        dialog.setContentView(root);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        this.arguments = arguments;
        this.host = host;

        if (savedInstanceState != null) {
            email = savedInstanceState.getString("email");
        }

        loadPage();
    }

    private int dp(int dp) {
        DisplayMetrics displayMetrics = root.getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @SuppressLint("SetJavaScriptEnabled")
    /*pkg*/ void loadPage() {
        try {
            Bundle parameters = arguments;
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

            webView.setWebViewClient(new OAuthWebViewClient(host));
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);

            // spy for email, part 1
            if (Build.VERSION.SDK_INT >= 19) {
                webView.addJavascriptInterface(new Object() {
                    @JavascriptInterface
                    public void setEmail(String email) {
                        VkOAuthDialogHolder.this.email = email;
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
            host.setResultAndFinish(Activity.RESULT_CANCELED, null);
        }
    }

    /*pkg*/ void onSaveInstanceState(final Bundle outState) {
        outState.putString("email", email);
    }

    /*pkg*/ void deliverResultToActivity(Activity activity) {
        VkApp
                .getInstance(arguments.getInt(VK_EXTRA_CLIENT_ID))
                .onActivityResult(cast(activity), arguments.getInt("request code"), resultCode, data);
    }

    private static <T extends Activity & VkApp.VkAuthCallbackProvider> T cast(Activity activity) {
        return (T) activity;
    }

    private interface Host {
        VkOAuthDialogHolder getHolder();
        void dismiss();
        void setResultAndFinish(int result, Intent data);
    }

    public static final class NativeFragment extends android.app.DialogFragment implements Host {

        private VkOAuthDialogHolder holder;

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            holder = new VkOAuthDialogHolder(getActivity(), getArguments(), savedInstanceState, this);
            return holder.dialog;
        }

        @Override public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            holder.onSaveInstanceState(outState);
        }

        @Override public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            deliverResult();
        }

        @Override public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            deliverResult();
        }

        private void deliverResult() {
            android.app.Fragment target = getTargetFragment();
            if (target == null) {
                holder.deliverResultToActivity(getActivity());
            } else {
                Bundle arguments = getArguments();
                target.onActivityResult(arguments.getInt("request code"), holder.resultCode, holder.data);
            }
        }

        @Override public VkOAuthDialogHolder getHolder() {
            return holder;
        }

        @Override public void setResultAndFinish(int result, Intent data) {
            holder.resultCode = result;
            holder.data = data;
            dismiss();
        }
    }

    public static final class CompatFragment extends android.support.v4.app.DialogFragment implements Host {

        private VkOAuthDialogHolder holder;

        @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            holder = new VkOAuthDialogHolder(getActivity(), getArguments(), savedInstanceState, this);
            return holder.dialog;
        }

        @Override public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            holder.onSaveInstanceState(outState);
        }

        @Override public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            deliverResult();
        }

        @Override public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            deliverResult();
        }

        private void deliverResult() {
            android.support.v4.app.Fragment target = getTargetFragment();
            if (target == null) {
                holder.deliverResultToActivity(getActivity());
            } else {
                Bundle arguments = getArguments();
                target.onActivityResult(arguments.getInt("request code"), holder.resultCode, holder.data);
            }
        }

        @Override public VkOAuthDialogHolder getHolder() {
            return holder;
        }

        @Override public void setResultAndFinish(int result, Intent data) {
            holder.resultCode = result;
            holder.data = data;
            dismiss();
        }
    }

    private static class OAuthWebViewClient extends WebViewClient {
        boolean canShowPage = true;
        final Host host;

        /*pkg*/ OAuthWebViewClient(Host host) {
            this.host = host;
        }

        boolean processUrl(String url) {
            if (url.startsWith(REDIRECT_URL)) {
                Intent data = new Intent(VK_RESULT_INTENT_NAME);
                String extraData = url.substring(url.indexOf('#') + 1);
                data.putExtra(VK_EXTRA_TOKEN_DATA, extraData);
                Map<String, String> resultParams = explodeQueryString(extraData);

                if (resultParams != null && (resultParams.containsKey(ERROR) || resultParams.containsKey(CANCEL))) {
                    host.setResultAndFinish(Activity.RESULT_CANCELED, data);
                } else {
                    host.setResultAndFinish(Activity.RESULT_OK, data);
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
                VkOAuthDialogHolder holder = host.getHolder();
                holder.progress.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);

                // spy for email, part 2
                if (Build.VERSION.SDK_INT >= 19) {
                    String email = holder.email;
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
                            host.getHolder().loadPage();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            host.dismiss();
                        }
                    });

            builder.show(); // was in try-catch O_o

        }
    }
}