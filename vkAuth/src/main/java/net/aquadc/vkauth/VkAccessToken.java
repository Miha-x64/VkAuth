package net.aquadc.vkauth;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.*;

/**
 * VK API access token for invoking API methods.
 */
public final class VkAccessToken implements Parcelable {

    private static final String AccessToken = "access_token";
    private static final String ExpiresIn = "expires_in";
    private static final String UserId = "user_id";
    private static final String Secret = "secret";
    private static final String Created = "created";
    private static final String Email = "email";
    private static final String Scope = "scope";

    @NonNull private final String accessToken;
    private final int ttlSeconds;
    @NonNull private final String userId;
    @Nullable private final String secret;
    private final long creationTimeMillis;
    @Nullable private final String email;
    @NonNull private final Set<VkScope> scope;

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

    private VkAccessToken(@NonNull String accessToken, int ttlSeconds, @NonNull String userId, @Nullable String secret,
                          long creationTimeMillis, @Nullable String email, @NonNull Set<VkScope> scope) {
        this.accessToken = accessToken;
        this.ttlSeconds = ttlSeconds;
        this.userId = userId;
        this.secret = secret;
        this.creationTimeMillis = creationTimeMillis;
        this.email = email;
        this.scope = scope;
    }

    private Map<String, String> tokenParams() {
        Map<String, String> params = new HashMap<>();
        params.put(AccessToken, accessToken);
        params.put(ExpiresIn, "" + ttlSeconds);
        params.put(UserId, userId);
        params.put(Created, "" + creationTimeMillis);
        if (!scope.isEmpty()) {
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

    @NonNull public String getAccessToken() {
        return accessToken;
    }

    public boolean isValid() {
        return !(ttlSeconds > 0 && ttlSeconds * 1000 + creationTimeMillis < System.currentTimeMillis());
    }

    @NonNull public String getUserId() {
        return userId;
    }

    /**
     * @deprecated useless thing for 'nohttps', should be avoided
     */
    @Nullable public String getSecret() {
        return secret;
    }

    @NonNull public Date getExpiryDate() {
        return new Date(creationTimeMillis + 1000 * ttlSeconds);
    }

    @Nullable public String getEmail() {
        return email;
    }

    @NonNull public Set<VkScope> getScope() {
        return scope;
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
                ", expiresIn=" + ttlSeconds +
                ", userId='" + userId + '\'' +
                ", secret='" + secret + '\'' +
                ", created=" + creationTimeMillis +
                ", email='" + email + '\'' +
                ", scope=" + scope +
                '}';
    }


    @Override public int describeContents() {
        return 0;
    }
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.accessToken);
        dest.writeInt(this.ttlSeconds);
        dest.writeString(this.userId);
        dest.writeString(this.secret);
        dest.writeLong(this.creationTimeMillis);
        dest.writeString(this.email);
        dest.writeInt(scope.size());
        for (VkScope s : scope) {
            dest.writeString(s.scopeName);
        }
    }
    public static final Creator<VkAccessToken> CREATOR = new Creator<VkAccessToken>() {
        @Override public VkAccessToken createFromParcel(Parcel in) {
            String accessToken = in.readString();
            int ttlSeconds = in.readInt();
            String userId = in.readString();
            String secret = in.readString();
            long creationTimeMillis = in.readLong();
            String email = in.readString();
            int size = in.readInt();
            String[] scope = new String[size];
            for (int i = 0; i < size; i++) {
                scope[i] = in.readString();
            }
            return new VkAccessToken(
                    accessToken, ttlSeconds, userId, secret, creationTimeMillis, email, VkScope.asSet(scope)
            );
        }
        @Override public VkAccessToken[] newArray(int size) {
            return new VkAccessToken[size];
        }
    };
}