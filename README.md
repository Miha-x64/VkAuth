# VkAuth [![](https://jitpack.io/v/TieToGather/VkAuth.svg)](https://jitpack.io/#TieToGather/VkAuth)

VK authentication library for Android.

* Much more lightweight than
[Official SDK](https://github.com/VKCOM/vk-android-sdk);
* much better designed (no static Context, no `registerObject()` and such things);
* we'd accept issues and PRs (official SDK are not).

# Features

* authentication via official app and WebView (if no app or if you choose to do so)
* Working with Activities, Fragments, and v4 Fragments

# Planned

* CAPTCHA and verification support

# Usage

Sample usage from Activity:
```java
public final class AuthActivity extends AppCompatActivity
        implements View.OnClickListener, VkApp.VkAuthCallback, VkApp.VkAuthCallbackProvider {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        findViewById(R.id.authButton).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        VkApp
                .getInstance(BuildConfig.VK_APP_ID)
                .login(this, EnumSet.noneOf(VkScope.class), AuthenticationWay.Auto, getSupportFragmentManager());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VkApp.getInstance(BuildConfig.VK_APP_ID).onActivityResult(this, requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public VkApp.VkAuthCallback getVkAuthCallback() {
        return this;
    }

    @Override
    public void onResult(VkAccessToken token) {
        // success!
    }

    @Override
    public void onError() {
        // sadness...
    }
}

```
