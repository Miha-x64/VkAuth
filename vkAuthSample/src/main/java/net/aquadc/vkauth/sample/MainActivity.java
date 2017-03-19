package net.aquadc.vkauth.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.aquadc.vkauth.VkAccessToken;
import net.aquadc.vkauth.VkApp;
import net.aquadc.vkauth.VkScope;
import net.aquadc.vkauth.WaitingForResult;

import java.util.EnumSet;

public final class MainActivity extends AppCompatActivity
        implements View.OnClickListener, WaitingForResult, VkApp.VkAuthCallback {

    private static final int RC_VK_AUTH = 1;

    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button authButton = (Button) findViewById(R.id.auth);
        authButton.setOnClickListener(this);

        output = (TextView) findViewById(R.id.output);
    }

    @Override
    public void onClick(View v) {
        VkApp.getInstance(BuildConfig.VK_APP_ID).login(this, EnumSet.noneOf(VkScope.class));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        VkApp.getInstance(BuildConfig.VK_APP_ID).onActivityResult(requestCode, resultCode, data, this);
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
