package net.aquadc.vkauth;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.*;

/**
 * Presents VK API access token that used for loading API methods and other stuff.
 */
public final class VkAccessToken implements Parcelable {

    private static final String AccessToken = "access_token";
    private static final String ExpiresIn = "expires_in";
    private static final String UserId = "user_id";
    private static final String Secret = "secret";
    private static final String Created = "created";
    private static final String Email = "email";
    private static final String Scope = "scope";

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

    @Nullable
    /*pkg*/ static VkAccessToken create(@Nullable Map<String, String> parameters) {
        // todo: real error-handling instead of returning nulls
        if (parameters == null) return null;

        String accessToken = parameters.get(AccessToken);
        if (accessToken == null || accessToken.isEmpty()) return null;

        String expiry = parameters.get(ExpiresIn);
        int expiresIn;
        try {
            expiresIn = expiry == null ? 0 : Integer.parseInt(expiry);
        } catch (NumberFormatException e) {
            return null;
        }
        if (expiresIn < 0) return null;

        String userId = parameters.get(UserId);
        if (userId == null || userId.isEmpty()) return null;

        String secret = parameters.get(Secret);

        String createdStr = parameters.get(Created);
        long created;
        if (createdStr != null) { // won't arrive from App/WebView, only from another token
            try {
                created = Long.parseLong(createdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            created = System.currentTimeMillis();
        }

        String email = parameters.get(Email);

        String scopeStr = parameters.get(Scope);
        Set<VkScope> scope;
        if (scopeStr != null && !scopeStr.isEmpty()) {
            try {
                scope = VkScope.asSet(scopeStr.split(","));
            } catch (NoSuchElementException e) {
                return null;
            }
        } else {
            scope = Collections.emptySet();
        }

        return new VkAccessToken(accessToken, expiresIn, userId, secret, created, email, scope);
    }

    private VkAccessToken(String accessToken, int expiresIn, String userId, @Nullable String secret, long created,
                          @Nullable String email, Set<VkScope> scope) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.secret = secret;
        this.created = created;
        this.email = email;
        this.scope = scope;
    }

    private Map<String, String> tokenParams() {
        Map<String, String> params = new HashMap<>();
        params.put(AccessToken, accessToken);
        params.put(ExpiresIn, "" + expiresIn);
        params.put(UserId, userId);
        params.put(Created, "" + created);
        if (scope != null) {
            params.put(Scope, TextUtils.join(",", scope));
        }

        if (secret != null) {
            params.put(Secret, secret);
        }
        if (email != null) {
            params.put(Email, email);
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

    @Nullable
    /*pkg*/ VkAccessToken overriddenBy(@NonNull VkAccessToken token) {
        Map<String, String> newTokenParams = tokenParams();
        newTokenParams.putAll(token.tokenParams());
        return create(newTokenParams);
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
            dest.writeString(s.scopeName);
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