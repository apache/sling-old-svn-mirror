/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.discovery.impl.topology.connector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.discovery.impl.Config;

/**
 * Request Validator helper.
 */
public class TopologyRequestValidator {

    public static final String SIG_HEADER = "X-SlingTopologyTrust";

    public static final String HASH_HEADER = "X-SlingTopologyHash";

    /**
     * Maximum number of keys to keep in memory.
     */
    private static final int MAXKEYS = 5;

    /**
     * Minimum number of keys to keep in memory.
     */
    private static final int MINKEYS = 3;

    /**
     * true if trust information should be in request headers.
     */
    private boolean trustEnabled;

    /**
     * true if encryption of the message payload should be encrypted.
     */
    private boolean encryptionEnabled;

    /**
     * map of hmac keys, keyed by key number.
     */
    private Map<Integer, Key> keys = new ConcurrentHashMap<Integer, Key>();

    /**
     * The shared key.
     */
    private String sharedKey;

    /**
     * TTL of each shared key generation.
     */
    private long interval;

    /**
     * If true, everything is deactivated.
     */
    private boolean deactivated;

    private SecureRandom random = new SecureRandom();

    /**
     * Create a TopologyRequestValidator.
     *
     * @param config the configuation object
     */
    public TopologyRequestValidator(Config config) {
        trustEnabled = false;
        encryptionEnabled = false;
        if (config.isHmacEnabled()) {
            trustEnabled = true;
            sharedKey = config.getSharedKey();
            interval = config.getKeyInterval();
            encryptionEnabled = config.isEncryptionEnabled();
        }
        deactivated = false;
    }

    /**
     * Encodes a request returning the encoded body
     *
     * @param body
     * @return the encoded body.
     * @throws IOException
     */
    public String encodeMessage(String body) throws IOException {
        checkActive();
        if (encryptionEnabled) {
            try {
                JSONObject json = new JSONObject();
                json.put("payload", new JSONArray(encrypt(body)));
                return json.toString();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            } catch (IllegalBlockSizeException e) {
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            } catch (BadPaddingException e) {
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            } catch (UnsupportedEncodingException e) {
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            } catch (NoSuchPaddingException e) {
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            } catch (JSONException e) {
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            } catch (InvalidKeySpecException e) {
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            } catch (InvalidParameterSpecException e) {
                throw new IOException("Unable to Encrypt Message " + e.getMessage());
            }

        }
        return body;
    }

    /**
     * Decode a message sent from the client.
     *
     * @param request the request object for the message.
     * @return the message in clear text.
     * @throws IOException if there is a problem decoding the message or the
     *             message is invalid.
     */
    public String decodeMessage(HttpServletRequest request) throws IOException {
        checkActive();
        return decodeMessage("request:", request.getRequestURI(), getRequestBody(request),
            request.getHeader(HASH_HEADER));
    }

    /**
     * Decode a response from the server.
     *
     * @param method the method that received the response.
     * @return the message in clear text.
     * @throws IOException if there was a problem decoding the message.
     */
    public String decodeMessage(HttpMethod method) throws IOException {
        checkActive();
        return decodeMessage("response:", method.getPath(), getResponseBody(method),
            getResponseHeader(method, HASH_HEADER));
    }

    /**
     * Decode a message
     *
     * @param prefix the prefix to indicate if the message is a request or
     *            response message.
     * @param url the url associated with the message.
     * @param body the body of the message.
     * @param requestHash a hash of the message.
     * @return the message in clear text
     * @throws IOException if the message can't be decrypted.
     */
    private String decodeMessage(String prefix, String url, String body, String requestHash)
            throws IOException {
        if (trustEnabled) {
            String bodyHash = hash(prefix + url + ":" + body);
            if (bodyHash.equals(requestHash)) {
                if (encryptionEnabled) {
                    try {
                        JSONObject json = new JSONObject(body);
                        if (json.has("payload")) {
                            return decrypt(json.getJSONArray("payload"));
                        }
                    } catch (JSONException e) {
                        throw new IOException("Encrypted Message is in the correct json format");
                    } catch (InvalidKeyException e) {
                        throw new IOException("Encrypted Message is in the correct json format");
                    } catch (IllegalBlockSizeException e) {
                        throw new IOException("Encrypted Message is in the correct json format");
                    } catch (BadPaddingException e) {
                        throw new IOException("Encrypted Message is in the correct json format");
                    } catch (NoSuchAlgorithmException e) {
                        throw new IOException("Encrypted Message is in the correct json format");
                    } catch (NoSuchPaddingException e) {
                        throw new IOException("Encrypted Message is in the correct json format");
                    } catch (InvalidAlgorithmParameterException e) {
                        throw new IOException("Encrypted Message is in the correct json format");
                    } catch (InvalidKeySpecException e) {
                        throw new IOException("Encrypted Message is in the correct json format");
                    }

                }
            }
            throw new IOException("Message is not valid, hash does not match message");
        }
        return body;
    }

    /**
     * Is the request from the client trusted, based on the signature headers.
     *
     * @param request the request.
     * @return true if trusted, or true if this component is disabled.
     */
    public boolean isTrusted(HttpServletRequest request) {
        checkActive();
        if (trustEnabled) {
            return checkTrustHeader(request.getHeader(HASH_HEADER),
                request.getHeader(SIG_HEADER));
        }
        return false;
    }

    /**
     * Is the response from the server to be trusted by the client.
     *
     * @param method the client method.
     * @return true if trusted, or true if this component is disabled.
     */
    public boolean isTrusted(HttpMethod method) {
        checkActive();
        if (trustEnabled) {
            return checkTrustHeader(getResponseHeader(method, HASH_HEADER),
                getResponseHeader(method, SIG_HEADER));
        }
        return false;
    }

    /**
     * Trust a message on the client before sending, only if trust is enabled.
     *
     * @param method the method which will have headers set after the call.
     * @param body the body.
     */
    public void trustMessage(HttpMethod method, String body) {
        checkActive();
        if (trustEnabled) {
            String bodyHash = hash("request:" + method.getPath() + ":" + body);
            method.setRequestHeader(HASH_HEADER, bodyHash);
            method.setRequestHeader(SIG_HEADER, createTrustHeader(bodyHash));
        }
    }

    /**
     * Trust a response message sent from the server to the client.
     *
     * @param response the response.
     * @param request the request,
     * @param body body of the response.
     */
    public void trustMessage(HttpServletResponse response, HttpServletRequest request, String body) {
        checkActive();
        if (trustEnabled) {
            String bodyHash = hash("response:" + request.getRequestURI() + ":" + body);
            response.setHeader(HASH_HEADER, bodyHash);
            response.setHeader(SIG_HEADER, createTrustHeader(bodyHash));
        }
    }

    /**
     * @param body
     * @return a hash of body base64 encoded.
     */
    private String hash(String toHash) {
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            return new String(Base64.encodeBase64(m.digest(toHash.getBytes("UTF-8"))), "UTF-8");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Generate a signature of the bodyHash and encode it so that it contains
     * the key number used to generate the signature.
     *
     * @param bodyHash a hash
     * @return the signature.
     */
    private String createTrustHeader(String bodyHash) {
        try {
            int keyNo = getCurrentKey();
            return keyNo + "/" + hmac(keyNo, bodyHash);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Check that the signature is a signature of the body hash.
     *
     * @param bodyHash the body hash.
     * @param signature the signature.
     * @return true if the signature can be trusted.
     */
    private boolean checkTrustHeader(String bodyHash, String signature) {
        try {
            if (bodyHash == null || signature == null ) {
                return false;
            }
            String[] parts = signature.split("/", 2);
            int keyNo = Integer.parseInt(parts[0]);
            return hmac(keyNo, bodyHash).equals(parts[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Get a Mac instance for the key number.
     *
     * @param keyNo the key number.
     * @return the mac instance.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     */
    private Mac getMac(int keyNo) throws NoSuchAlgorithmException, InvalidKeyException,
            UnsupportedEncodingException {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(getKey(keyNo));
        return m;
    }

    /**
     * Perform a HMAC on the body using the key specified.
     *
     * @param keyNo the key number.
     * @param bodyHash a hash of the body.
     * @return the hmac signature.
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws IllegalStateException
     * @throws NoSuchAlgorithmException
     */
    private String hmac(int keyNo, String bodyHash) throws InvalidKeyException,
            UnsupportedEncodingException, IllegalStateException, NoSuchAlgorithmException {
        return new String(Base64.encodeBase64(getMac(keyNo).doFinal(bodyHash.getBytes("UTF-8"))),
            "UTF-8");
    }

    /**
     * Decrypt the body.
     *
     * @param jsonArray the encrypted payload
     * @return the decrypted payload.
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     * @throws InvalidAlgorithmParameterException
     * @throws JSONException
     */
    private String decrypt(JSONArray jsonArray) throws IllegalBlockSizeException,
            BadPaddingException, UnsupportedEncodingException, InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException, JSONException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, getCiperKey(Base64.decodeBase64(jsonArray.getString(0).getBytes("UTF-8"))), new IvParameterSpec(Base64.decodeBase64(jsonArray.getString(1).getBytes("UTF-8"))));
        return new String(cipher.doFinal(Base64.decodeBase64(jsonArray.getString(2).getBytes("UTF-8"))));
    }

    /**
     * Encrypt a payload with the numbed key/
     *
     * @param payload the payload.
     * @param keyNo the key number.
     * @return an encrypted version.
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     * @throws InvalidParameterSpecException
     */
    private List<String> encrypt(String payload) throws IllegalBlockSizeException,
            BadPaddingException, UnsupportedEncodingException, InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidParameterSpecException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] salt = new byte[9];
        random.nextBytes(salt);
        cipher.init(Cipher.ENCRYPT_MODE, getCiperKey(salt));
        AlgorithmParameters params = cipher.getParameters();
        List<String> encrypted = new ArrayList<String>();
        encrypted.add(new String(Base64.encodeBase64(salt)));
        encrypted.add(new String(Base64.encodeBase64(params.getParameterSpec(IvParameterSpec.class).getIV())));
        encrypted.add(new String(Base64.encodeBase64(cipher.doFinal(payload.getBytes("UTF-8")))));
        return encrypted;
    }

    /**
     * @param salt number of the key.
     * @return the CupherKey.
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private Key getCiperKey(byte[] salt) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        // hashing the password 65K times takes 151ms, hashing 256 times takes 2ms.
        // Since the salt has 2^^72 values, 256 times is probably good enough.
        KeySpec spec = new PBEKeySpec(sharedKey.toCharArray(), salt, 256, 128);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey key = new SecretKeySpec(tmp.getEncoded(), "AES");
        return key;
    }

    /**
     * @param keyNo number of the key.
     * @return the HMac key.
     * @throws UnsupportedEncodingException
     */
    private Key getKey(int keyNo) throws UnsupportedEncodingException {
        if(Math.abs(keyNo - getCurrentKey()) > 1 ) {
            throw new IllegalArgumentException("Key has expired");
        }
        if (keys.containsKey(keyNo)) {
            return keys.get(keyNo);
        }
        trimKeys();
        SecretKeySpec key = new SecretKeySpec(hash(sharedKey + keyNo).getBytes("UTF-8"),
            "HmacSHA256");
        keys.put(keyNo, key);
        return key;
    }

    private int getCurrentKey() {
        return (int) (System.currentTimeMillis() / interval);
    }

    /**
     * dump olf keys.
     */
    private void trimKeys() {
        if (keys.size() > MAXKEYS) {
            List<Integer> keysKeys = new ArrayList<Integer>(keys.keySet());
            Collections.sort(keysKeys);
            for (Integer k : keysKeys) {
                if (keys.size() < MINKEYS) {
                    break;
                }
                keys.remove(k);
            }
        }

    }

    /**
     * Get the value of a response header.
     *
     * @param method the method
     * @param name the name of the response header.
     * @return the value of the response header, null if none.
     */
    private String getResponseHeader(HttpMethod method, String name) {
        Header h = method.getResponseHeader(name);
        if (h == null) {
            return null;
        }
        return h.getValue();
    }

    /**
     * Get the request body.
     *
     * @param request the request.
     * @return the body as a string.
     * @throws IOException
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        return IOUtils.toString(request.getReader());
    }

    /**
     * @param method the response method.
     * @return the body of the response from the server.
     * @throws IOException
     */
    private String getResponseBody(HttpMethod method) throws IOException {
        if (method instanceof HttpMethodBase) {
            return ((HttpMethodBase) method).getResponseBodyAsString(16 * 1024 * 1024);
        }
        return method.getResponseBodyAsString();
    }

    /**
     * throw an exception if not active.
     */
    private void checkActive() {
        if (deactivated) {
            throw new IllegalStateException(this.getClass().getName() + " is not active");
        }
        if ((trustEnabled || encryptionEnabled) && sharedKey == null) {
            throw new IllegalStateException(this.getClass().getName()
                + " Shared Key must be set if encryption or signing is enabled.");
        }
    }

}
