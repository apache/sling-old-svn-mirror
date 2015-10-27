/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.resourceresolver.impl.mapping;

/**
 * Bloom filter utilities.
 */
public class BloomFilterUtils {

    /**
     * The number of bits needed per stored element.
     * Using the formula m = - (n * ln(p)) / (ln(2)^2) as described in
     * http://en.wikipedia.org/wiki/Bloom_filter
     * (simplified, as we used a fixed K: 2).
     */
    private static final double BIT_FACTOR = -Math.log(0.02) / Math.pow(Math.log(2), 2);

    /**
     * Create a bloom filter array for the given number of elements.
     *
     * @param elementCount the number of entries
     * @param maxBytes the maximum number of bytes
     * @return the empty bloom filter
     */
    public static byte[] createFilter(int elementCount, int maxBytes) {
        int bits = (int) (elementCount * BIT_FACTOR) + 7;
        return new byte[Math.min(maxBytes, bits / 8)];
    }

    /**
     * Add the key.
     *
     * @param bloom the bloom filter
     * @param key the key
     */
    public static void add(byte[] bloom, Object key) {
        int len = bloom.length;
        if (len > 0) {
            int h1 = hash(key.hashCode()), h2 = hash(h1);
            bloom[(h1 >>> 3) % len] |= 1 << (h1 & 7);
            bloom[(h2 >>> 3) % len] |= 1 << (h2 & 7);
        }
    }

    /**
     * Remove the key.
     *
     * @param bloom the bloom filter
     * @param key the key
     */
    public static void remove(byte[] bloom, Object key){
        throw new UnsupportedOperationException();
    }

    /**
     * Check whether the given key is probably in the set. This method never
     * returns false if the key is in the set, but possibly returns true even if
     * it isn't.
     *
     * @param bloom the bloom filter
     * @param key the key
     * @return true if the given key is probably in the set
     */
    public static boolean probablyContains(byte[] bloom, Object key) {
        int len = bloom.length;
        if (len == 0) {
            return true;
        }
        int h1 = hash(key.hashCode());
        long x = bloom[(h1 >>> 3) % len] & (1 << (h1 & 7));
        if (x != 0) {
            int h2 = hash(h1);
            x = bloom[(h2 >>> 3) % len] & (1 << (h2 & 7));
        }
        return x != 0;
    }

    /**
     * Get the hash value for the given key. The returned hash value is
     * stretched so that it should work well even for relatively bad hashCode
     * implementations.
     *
     * @param key the key
     * @return the hash value
     */
    private static int hash(int key) {
        int hash = key;
        // a supplemental secondary hash function
        // to protect against hash codes that don't differ much
        hash = ((hash >>> 16) ^ hash) * 0x45d9f3b;
        hash = ((hash >>> 16) ^ hash) * 0x45d9f3b;
        hash = (hash >>> 16) ^ hash;
        return hash;
    }

}
