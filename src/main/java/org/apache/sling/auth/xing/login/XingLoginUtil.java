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
package org.apache.sling.auth.xing.login;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.auth.xing.api.XingUser;

public class XingLoginUtil {

    public static String getHash(Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            final SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
            final Object attribute = simpleCredentials.getAttribute(XingLogin.AUTHENTICATION_CREDENTIALS_HASH_KEY);
            if (attribute instanceof String) {
                return (String) attribute;
            }
        }
        return null;
    }

    public static String getUser(Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            final SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
            final Object attribute = simpleCredentials.getAttribute(XingLogin.AUTHENTICATION_CREDENTIALS_USERDATA_KEY);
            if (attribute instanceof String) {
                try {
                    final String base64Json = (String) attribute;
                    final byte[] decoded = Base64.decodeBase64(base64Json);
                    return new String(decoded, "UTF-8");
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return null;
    }

    public static XingUser fromJson(final String json) {
        final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        return gson.fromJson(json, XingUser.class);
    }

    public static String hash(final String json, final String secretKey, final String hashAlgorithm) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
        final Gson gson = new Gson();
        final List<String> list = new ArrayList<String>();
        final Map map = gson.fromJson(json, Map.class);
        join(map, list, "");
        Collections.sort(list);
        final String message = StringUtils.join(list, null);
        final byte[] result = hmac(message, secretKey, hashAlgorithm);
        return Hex.encodeHexString(result);
    }

    private static void join(final Map map, final List<String> list, final String prefix) {
        final Set<Map.Entry> entrySet = map.entrySet();
        for (Map.Entry entry : entrySet) {
            if (entry.getValue() instanceof Map) {
                final Map m = (Map) entry.getValue();
                join(m, list, prefix.concat(entry.getKey().toString()));
            } else {
                list.add(prefix.concat(entry.getKey().toString()).concat(entry.getValue().toString()));
            }
        }
    }

    private static byte[] hmac(final String message, final String secretKey, final String hashAlgorithm) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        final SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), hashAlgorithm);
        final Mac mac = Mac.getInstance(hashAlgorithm);
        mac.init(secretKeySpec);
        return mac.doFinal(message.getBytes("UTF-8"));
    }

}
