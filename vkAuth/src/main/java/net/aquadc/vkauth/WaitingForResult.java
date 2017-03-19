package net.aquadc.vkauth;

import android.app.FragmentManager;
import android.content.Intent;
import android.support.annotation.RequiresPermission;

/**
 * Created by mike on 21.02.17
 */

public interface WaitingForResult {
    void onActivityResult(int requestCode, int resultCode, Intent data);
    void startActivityForResult(@RequiresPermission Intent intent, int requestCode);
    FragmentManager getFragmentManager();
}
