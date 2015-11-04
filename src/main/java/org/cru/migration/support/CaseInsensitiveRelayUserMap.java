package org.cru.migration.support;

/**
 * author@lee.braddock
 */
import org.cru.migration.domain.RelayUser;

import java.util.concurrent.ConcurrentHashMap;

public class CaseInsensitiveRelayUserMap extends ConcurrentHashMap<String, RelayUser> {

    public RelayUser put(String key, RelayUser relayUser) {
        return super.put(key.toLowerCase(), relayUser);
    }

    public RelayUser get(String key) {
        return super.get(key.toLowerCase());
    }
}