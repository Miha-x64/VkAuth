package net.aquadc.vkauth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 21.02.17
 */
/*pkg*/ final class Util {
    private Util() {}

    // from VK SDK
    /*pkg*/ static String[] getCertificateFingerprints(Context ctx, String packageName)
            throws PackageManager.NameNotFoundException {
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

    // from SO answer: https://stackoverflow.com/a/9855338/3050249
    private static final char[] HexAlphabet = "0123456789ABCDEF".toCharArray();
    private static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HexAlphabet[v >>> 4];
            hexChars[j * 2 + 1] = HexAlphabet[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Nullable // from VK SDK
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

    /*pkg*/ static int dp(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /*pkg*/ static void required(Object o, String name) {
        if (o == null) throw new NullPointerException(name + " is required, null given");
    }
    /*pkg*/ static void required(Object o0, String n0, Object o1, String n1) {
        required(o0, n0);
        required(o1, n1);
    }
    /*pkg*/ static void required(Object o0, String n0, Object o1, String n1, Object o2, String n2, Object o3, String n3) {
        required(o0, n0);
        required(o1, n1);
        required(o2, n2);
        required(o3, n3);
    }

}
