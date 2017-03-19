package net.aquadc.vkauth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 21.02.17
 */
@VisibleForTesting
public final class Util {
    private Util() {}

    // from VK SDK
    @VisibleForTesting
    public static String[] getCertificateFingerprints(Context ctx, String packageName) throws PackageManager.NameNotFoundException {
        @SuppressLint("PackageManagerGetSignatures")
        Signature[] signatures =
                ctx.getPackageManager()
                        .getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;
        String[] result = new String[signatures.length];

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA is not available");
        }

        for (int i = 0, size = signatures.length; i < size; i++) {
            md.update(signatures[i].toByteArray());
            result[i] = toHex(md.digest());
        }
        return result;
    }

    @Deprecated // todo replace me please
    private static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    /*pkg*/ static boolean isAppInstalled(Context context, String appId) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(appId, 0); // todo check if works, was GET ACTIVITIES
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    @Nullable
    /*pkg*/ static Map<String, String> explodeQueryString(@Nullable String queryString) {
        if (queryString == null) {
            return null;
        }
        String[] keyValuePairs = queryString.split("&");
        HashMap<String, String> parameters = new HashMap<>(keyValuePairs.length);
        for (String keyValueString : keyValuePairs) {
            String[] keyValueArray = keyValueString.split("=");
            parameters.put(keyValueArray[0], keyValueArray[1]);
        }
        return parameters;
    }

}
