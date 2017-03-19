package net.aquadc.vkauth;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Presents VK API access token that used for loading API methods and other stuff.
 */
public final class VkAccessToken implements Parcelable {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";
    private static final String USER_ID = "user_id";
    private static final String SECRET = "secret";
    private static final String HTTPS_REQUIRED = "https_required";
    private static final String CREATED = "created";
    private static final String EMAIL = "email";
    private static final String SCOPE = "scope";

    // String token for use in request parameters
    private final String accessToken;

    // Seconds from 'created' when token will expire
    private final int expiresIn;

    // Current user id for this token
    private final String userId;

    // User secret to sign requests (if nohttps used)
    @Nullable
    private final String secret;

    // Indicates time of token creation
    private final long created; // millis

    // User email
    private final String email;

    // Token scope
    private final Set<VkScope> scope;

    /*pkg*/ VkAccessToken(@Nullable Map<String, String> parameters) {
        if (parameters == null) {
            throw new NullPointerException();
        }
        if (parameters.size() == 0) {
            throw new IllegalArgumentException();
        }

        this.accessToken = required(parameters.get(ACCESS_TOKEN), "access token");

        String expiry = parameters.get(EXPIRES_IN);
        this.expiresIn = expiry == null ? 0 : Integer.parseInt(expiry);

        this.userId = required(parameters.get(USER_ID), "user ID");
        this.secret = parameters.get(SECRET);

        String created = parameters.get(CREATED);
        if (created != null) {
            this.created = Long.parseLong(created);
        } else {
            this.created = System.currentTimeMillis();
        }

        this.email = parameters.get(EMAIL);

        String scope = parameters.get(SCOPE);
        if (scope != null && !scope.isEmpty()) {
            this.scope = VkScope.asSet(scope.split(","));
        } else {
            this.scope = Collections.emptySet();
        }
    }

    private Map<String, String> tokenParams() {
        Map<String, String> params = new HashMap<>();
        params.put(ACCESS_TOKEN, accessToken);
        params.put(EXPIRES_IN, "" + expiresIn);
        params.put(USER_ID, userId);
        params.put(CREATED, "" + created);
        if (scope != null) {
            params.put(SCOPE, TextUtils.join(",", scope));
        }

        if (secret != null) {
            params.put(SECRET, secret);
        }
        params.put(HTTPS_REQUIRED, "1");
        if (email != null) {
            params.put(EMAIL, email);
        }
        return params;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getSecret() {
        return secret;
    }

    public boolean isValid() {
        return !(expiresIn > 0 && expiresIn * 1000 + created < System.currentTimeMillis());
    }

    /*pkg*/ VkAccessToken overriddenBy(@NonNull VkAccessToken token) {
        Map<String, String> newTokenParams = tokenParams();
        newTokenParams.putAll(token.tokenParams());
        return new VkAccessToken(newTokenParams);
    }

    private static <T> T required(T t, String message) {
        if (t == null) {
            throw new NullPointerException(message + " is required");
        }
        return t;
    }

    @Override
    public String toString() {
        return "VKAccessToken{" +
                "accessToken='" + accessToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", userId='" + userId + '\'' +
                ", secret='" + secret + '\'' +
                ", created=" + created +
                ", email='" + email + '\'' +
                ", scope=" + scope +
                '}';
    }

    public Date getExpiryDate() {
        return new Date(created + 1000 * expiresIn);
    }


    @Override public int describeContents() {
        return 0;
    }
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.accessToken);
        dest.writeInt(this.expiresIn);
        dest.writeString(this.userId);
        dest.writeString(this.secret);
        dest.writeLong(this.created);
        dest.writeString(this.email);
        dest.writeInt(scope.size());
        for (VkScope s : scope) {
            dest.writeString(s.name());
        }
    }
    /*pkg*/ VkAccessToken(Parcel in) {
        this.accessToken = in.readString();
        this.expiresIn = in.readInt();
        this.userId = in.readString();
        this.secret = in.readString();
        this.created = in.readLong();
        this.email = in.readString();
        int size = in.readInt();
        String[] scope = new String[size];
        for (int i = 0; i < size; i++) {
            scope[i] = in.readString();
        }
        this.scope = VkScope.asSet(scope);
    }
    public static final Creator<VkAccessToken> CREATOR = new Creator<VkAccessToken>() {
        @Override public VkAccessToken createFromParcel(Parcel source) {
            return new VkAccessToken(source);
        }
        @Override public VkAccessToken[] newArray(int size) {
            return new VkAccessToken[size];
        }
    };
}