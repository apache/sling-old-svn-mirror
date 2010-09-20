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
package org.apache.sling.installer.it;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.installer.api.InstallableResource;

public class MockInstallableResource extends InstallableResource {

    private static int counter;

    public MockInstallableResource(String uri) {
        this(uri, "", null);
    }

    public MockInstallableResource(String uri, String data, String digest) {
        super(uri, new ByteArrayInputStream(data.getBytes()),
                null, getNextDigest(digest),
                InstallableResource.TYPE_BUNDLE, null);
    }

    public MockInstallableResource(String uri, InputStream is, String digest, String type, Integer priority) {
        super(uri, is,
                null, digest,
                type != null ? type : InstallableResource.TYPE_BUNDLE, priority);
    }

    public MockInstallableResource(String uri, Dictionary<String, Object> d, String digest,String type, Integer priority) {
        super(uri, null,
                d, digest != null ? digest : computeDigest(d),
                type != null ? type : InstallableResource.TYPE_CONFIG, priority);
    }

    static String getNextDigest(String digest) {
        if(digest != null) {
            return digest;
        }
        synchronized (MockInstallableResource.class) {
            return String.valueOf(System.currentTimeMillis() + (counter++));
        }
    }

    /** The digest to be used. */
    private static final String DIGEST_TYPE = "MD5";

    /** convert digest to readable string (http://www.javalobby.org/java/forums/t84420.html) */
    private static String digestToString(MessageDigest d) {
        final BigInteger bigInt = new BigInteger(1, d.digest());
        return new String(bigInt.toString(16));
    }

    /** Compute digest on all keys of supplied data */
    private static String computeDigest(Dictionary<String, Object> data) {
        try {
            final MessageDigest d = MessageDigest.getInstance(DIGEST_TYPE);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);

            final SortedSet<String> sortedKeys = new TreeSet<String>();
            if(data != null) {
                for(Enumeration<String> e = data.keys(); e.hasMoreElements(); ) {
                    final String key = e.nextElement();
                    sortedKeys.add(key);
                }
            }
            for(String key : sortedKeys) {
                oos.writeObject(key);
                oos.writeObject(data.get(key));
            }

            bos.flush();
            d.update(bos.toByteArray());
            return digestToString(d);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
