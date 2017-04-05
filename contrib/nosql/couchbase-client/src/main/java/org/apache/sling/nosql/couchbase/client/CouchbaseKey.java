/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.nosql.couchbase.client;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import aQute.bnd.annotation.ProviderType;

/**
 * Builds couchbase keys. Cuts them off and replaces the remaining part with a hash if too long.
 */
@ProviderType
public final class CouchbaseKey {

    static final int MAX_KEY_LENGTH = 250;

    private CouchbaseKey() {
        // static methods only
    }

    /**
     * Builds a key for a couchbase document with the given prefix that does not
     * exceed the max. length for couchbase keys.
     * @param key Full key
     * @param keyPrefix Prefix to add before the key
     * @return Valid key for couchbase
     */
    public static String build(String key, String keyPrefix) {
        String cacheKey = keyPrefix + key;

        if (cacheKey.length() < MAX_KEY_LENGTH) {
            return cacheKey;
        }

        int charactersToKeep = MAX_KEY_LENGTH - keyPrefix.length() - 41;

        String toKeep = key.substring(0, charactersToKeep);
        String toHash = key.substring(charactersToKeep);

        String hash = calculateHash(toHash);

        return keyPrefix + toKeep + "#" + hash;
    }

    private static String calculateHash(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(message.getBytes("UTF-8"));
            byte[] digestBytes = digest.digest();

            return javax.xml.bind.DatatypeConverter.printHexBinary(digestBytes).toLowerCase();
        }
        catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Failed to create sha1 Hash from " + message, ex);
        }
        catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Failed to create sha1 Hash from " + message, ex);
        }
    }

}
