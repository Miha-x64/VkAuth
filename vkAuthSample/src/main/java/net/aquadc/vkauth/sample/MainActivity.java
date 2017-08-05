package net.aquadc.vkauth.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.aquadc.vkauth.*;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends AppCompatActivity
        implements View.OnClickListener, VkApp.VkAuthCallback, VkApp.VkAuthCallbackProvider {

    private Map<AuthenticationWay, Button> buttons;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttons = new EnumMap<>(AuthenticationWay.class);
        buttons.put(AuthenticationWay.OfficialVkApp, withOnClick(R.id.ofAppAuth));
        buttons.put(AuthenticationWay.WebView, withOnClick(R.id.webViewAuth));
        buttons.put(AuthenticationWay.Auto, withOnClick(R.id.autoAuth));

        output = (TextView) findViewById(R.id.output);
    }

    private Button withOnClick(@IdRes int id) {
        Button button = (Button) findViewById(id);
        button.setOnClickListener(this);
        return button;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // find out which auth ways are available, enable according buttons
        Set<AuthenticationWay> available = VkApp.getInstance(BuildConfig.VK_APP_ID).getAvailableAuthenticationWays(this);

        for (AuthenticationWay way : AuthenticationWay.values()) {
            buttons.get(way).setEnabled(available.contains(way));
        }
    }

    @Override
    public void onClick(View v) {
        for (Map.Entry<AuthenticationWay, Button> entry : buttons.entrySet()) {
            if (entry.getValue() == v) { // find way of clicked button
                VkApp.getInstance(BuildConfig.VK_APP_ID).login(this, EnumSet.noneOf(VkScope.class), entry.getKey(), getSupportFragmentManager());
                return;
            }
        }
        throw new UnsupportedOperationException("can't handle this click");
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
        output.setText(token.toString());
    }

    @Override
    public void onError() {
        Toast.makeText(this, "Sadness.", Toast.LENGTH_SHORT).show();
    }
}
