package net.aquadc.vkauth;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static net.aquadc.vkauth.Util.required;

/**
 * Created by mike on 21.02.17
 */

public enum VkScope {
    NOTIFY("notify"),
    FRIENDS("friends"),
    PHOTOS("photos"),
    AUDIO("audio"),
    VIDEO("video"),
    DOCS("docs"),
    NOTES("notes"),
    PAGES("pages"),
    STATUS("status"),
    WALL("wall"),
    GROUPS("groups"),
    MESSAGES("messages"),
    NOTIFICATIONS("notifications"),
    STATS("stats"),
    ADS("ads"),
    OFFLINE("offline"),
    EMAIL("email"),
    /** @deprecated can't see any reason to use it */ @Deprecated NOHTTPS("nohttps"),
    DIRECT("direct");

    private static final VkScope[] V = values();

    /*pkg*/ final String scopeName;

    VkScope(String scopeName) {
        this.scopeName = scopeName;
    }

    public static Set<VkScope> asSet(VkScope... scope) {
        required(scope, "scope");
        if (scope.length == 0) return EnumSet.noneOf(VkScope.class);
        return EnumSet.copyOf(Arrays.asList(scope));
    }

    /**
     * @throws NoSuchElementException when meets unknown scope
     */
    /*pkg*/ static Set<VkScope> asSet(String[] scope) {
        int size = scope.length;
        VkScope[] vkScope = new VkScope[size];
        for (int i = 0; i < size; i++) {
            vkScope[i] = VkScope.byScopeName(scope[i]);
        }
        return asSet(vkScope);
    }

    /**
     * @throws NoSuchElementException when meets unknown scope
     */
    /*pkg*/ static VkScope byScopeName(String scopeName) {
        for (VkScope s : V) {
            if (s.scopeName.equals(scopeName)) {
                return s;
            }
        }
        throw new NoSuchElementException("Scope for name " + scopeName + " was not found.");
    }

    /*pkg*/ static String joined(Set<VkScope> set) {
        // similar to TextUtils.join
        StringBuilder sb = new StringBuilder();
        Iterator<VkScope> it = set.iterator();
        if (it.hasNext()) {
            sb.append(it.next().scopeName);
            while (it.hasNext()) {
                sb.append(',');
                sb.append(it.next().scopeName);
            }
        }
        return sb.toString();
    }
}
