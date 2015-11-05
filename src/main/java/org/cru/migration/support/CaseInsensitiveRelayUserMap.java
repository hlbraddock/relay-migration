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

    @Override
    public RelayUser get(Object key) {
        String keyString = (String) key;
        return super.get(keyString.toLowerCase());
    }
}