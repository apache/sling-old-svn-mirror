/*
 * Licensed to the Sakai Foundation (SF) under one
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
package org.apache.sling.auth.form.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>TokenStore</code> class provides the secure token hash
 * implementation used by the {@link FormAuthenticationHandler} to generate,
 * validate and persist secure tokens.
 */
class TokenStore {

    /**
     * Array of hex characters used by {@link #byteToHex(byte[])} to convert a
     * byte array to a hex string.
     */
    private static final char[] TOHEX = "0123456789abcdef".toCharArray();

    /**
     * Name of the <code>SecureRandom</code> generator algorithm
     */
    private static final String SHA1PRNG = "SHA1PRNG";

    /**
     * The name of the HMAC function to calculate the hash code of the payload
     * with the secure token.
     */
    private static final String HMAC_SHA1 = "HmacSHA1";

    /**
     * String encoding to convert byte arrays to strings and vice-versa.
     */
    private static final String UTF_8 = "UTF-8";

    /** The number of secret keys in the token buffer currentTokens */
    private static final int TOKEN_BUFFER_SIZE = 5;

    public final Logger log = LoggerFactory.getLogger(TokenStore.class);

    /**
     * The ttl of the cookie before it becomes invalid (in ms)
     */
    private final long ttl;

    /**
     * The time when a new token should be created.
     */
    private long nextUpdate = System.currentTimeMillis();

    /**
     * The location of the current token.
     */
    private volatile int currentToken = 0;

    /**
     * A ring of tokens used to encrypt.
     */
    private volatile SecretKey[] currentTokens;

    /**
     * A secure random used for generating new tokens.
     */
    private SecureRandom random;

    /** The token file to persist the secure tokens */
    private File tokenFile;

    /** A temporary file used to update the secure token file */
    private File tmpTokenFile;

    /**
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws IllegalStateException
     * @throws NullPointerException if <code>tokenFile</code> is
     *             <code>null</code>.
     */
    TokenStore(final File tokenFile, final long sessionTimeout,
            final boolean fastSeed) throws NoSuchAlgorithmException,
            InvalidKeyException, IllegalStateException,
            UnsupportedEncodingException {

        if (tokenFile == null) {
            throw new NullPointerException("tokenfile");
        }

        this.random = SecureRandom.getInstance(SHA1PRNG);
        this.ttl = sessionTimeout;
        this.tokenFile = tokenFile;
        this.tmpTokenFile = new File(tokenFile + ".tmp");

        // prime the secret keys from persistence
        loadTokens();

        // warm up the crypto API
        if (fastSeed) {
            random.setSeed(getFastEntropy());
        } else {
            log.info("Seeding the secure random number generator can take "
                + "up to several minutes on some operating systems depending "
                + "upon environment factors. If this is a problem for you, "
                + "set the system property 'java.security.egd' to "
                + "'file:/dev/./urandom' or enable the Fast Seed Generator "
                + "in the Web Console");
        }
        byte[] b = new byte[20];
        random.nextBytes(b);
        final SecretKey secretKey = new SecretKeySpec(b, HMAC_SHA1);
        final Mac m = Mac.getInstance(HMAC_SHA1);
        m.init(secretKey);
        m.update(UTF_8.getBytes(UTF_8));
        m.doFinal();
    }

    /**
     * @param expires
     * @param userId
     * @return
     * @throws UnsupportedEncodingException
     * @throws IllegalStateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    String encode(final long expires, final String userId)
            throws IllegalStateException, UnsupportedEncodingException,
            NoSuchAlgorithmException, InvalidKeyException {
        int token = getActiveToken();
        SecretKey key = currentTokens[token];
        return encode(expires, userId, token, key);
    }

    private String encode(final long expires, final String userId,
            final int token, final SecretKey key) throws IllegalStateException,
            UnsupportedEncodingException, NoSuchAlgorithmException,
            InvalidKeyException {

        String cookiePayload = String.valueOf(token) + String.valueOf(expires)
            + "@" + userId;
        Mac m = Mac.getInstance(HMAC_SHA1);
        m.init(key);
        m.update(cookiePayload.getBytes(UTF_8));
        String cookieValue = byteToHex(m.doFinal());
        return cookieValue + "@" + cookiePayload;
    }

    /**
     * Splits the authentication data into the three parts packed together while
     * encoding the cookie.
     *
     * @param authData The authentication data to split in three parts
     * @return A string array with three elements being the three parts of the
     *         cookie value or <code>null</code> if the input is
     *         <code>null</code> or if the string does not contain (at least)
     *         three '@' separated parts.
     */
    static String[] split(final String authData) {
        String[] parts = StringUtils.split(authData, "@", 3);
        if (parts != null && parts.length == 3) {
            return parts;
        }
        return null;
    }

    /**
     * Returns <code>true</code> if the <code>value</code> is a valid secure
     * token as follows:
     * <ul>
     * <li>The string is not <code>null</code></li>
     * <li>The string contains three fields separated by an @ sign</li>
     * <li>The expiry time encoded in the second field has not yet passed</li>
     * <li>The hashing the third field, the expiry time and token number with
     * the secure token (indicated by the token number) gives the same value as
     * contained in the first field</li>
     * </ul>
     * <p>
     * Otherwise the method returns <code>false</code>.
     */
    boolean isValid(String value) {
        String[] parts = split(value);
        if (parts != null) {

            // single digit token number
            int tokenNumber = parts[1].charAt(0) - '0';
            if (tokenNumber >= 0 && tokenNumber < currentTokens.length) {

                long cookieTime = Long.parseLong(parts[1].substring(1));
                if (System.currentTimeMillis() < cookieTime) {

                    try {
                        SecretKey secretKey = currentTokens[tokenNumber];
                        String hmac = encode(cookieTime, parts[2], tokenNumber,
                            secretKey);
                        return value.equals(hmac);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        log.error(e.getMessage(), e);
                    } catch (InvalidKeyException e) {
                        log.error(e.getMessage(), e);
                    } catch (IllegalStateException e) {
                        log.error(e.getMessage(), e);
                    } catch (UnsupportedEncodingException e) {
                        log.error(e.getMessage(), e);
                    } catch (NoSuchAlgorithmException e) {
                        log.error(e.getMessage(), e);
                    }

                    log.error("AuthNCookie value '{}' is invalid", value);

                } else {
                    log.error("AuthNCookie value '{}' has expired {}ms ago",
                        value, (System.currentTimeMillis() - cookieTime));
                }

            } else {
                log.error(
                    "AuthNCookie value '{}' is invalid: refers to an invalid token number",
                    value, tokenNumber);
            }

        } else {
            log.error("AuthNCookie value '{}' has invalid format", value);
        }

        // failed verification, reason is logged
        return false;
    }

    /**
     * Maintain a circular buffer to tokens, and return the current one.
     *
     * @return the current token.
     */
    private synchronized int getActiveToken() {
        if (System.currentTimeMillis() > nextUpdate
            || currentTokens[currentToken] == null) {
            // cycle so that during a typical ttl the tokens get completely
            // refreshed.
            nextUpdate = System.currentTimeMillis() + ttl
                / (currentTokens.length - 1);
            byte[] b = new byte[20];
            random.nextBytes(b);

            SecretKey newToken = new SecretKeySpec(b, HMAC_SHA1);
            int nextToken = currentToken + 1;
            if (nextToken == currentTokens.length) {
                nextToken = 0;
            }
            currentTokens[nextToken] = newToken;
            currentToken = nextToken;
            saveTokens();
        }
        return currentToken;
    }

    /**
     * Stores the current set of tokens to the token file
     */
    private void saveTokens() {
        FileOutputStream fout = null;
        DataOutputStream keyOutputStream = null;
        try {
            File parent = tokenFile.getAbsoluteFile().getParentFile();
            log.info("Token File {} parent {} ", tokenFile, parent);
            if (!parent.exists()) {
                parent.mkdirs();
            }
            fout = new FileOutputStream(tmpTokenFile);
            keyOutputStream = new DataOutputStream(fout);
            keyOutputStream.writeInt(currentToken);
            keyOutputStream.writeLong(nextUpdate);
            for (int i = 0; i < currentTokens.length; i++) {
                if (currentTokens[i] == null) {
                    keyOutputStream.writeInt(0);
                } else {
                    keyOutputStream.writeInt(1);
                    byte[] b = currentTokens[i].getEncoded();
                    keyOutputStream.writeInt(b.length);
                    keyOutputStream.write(b);
                }
            }
            keyOutputStream.close();
            tmpTokenFile.renameTo(tokenFile);
        } catch (IOException e) {
            log.error("Failed to save cookie keys " + e.getMessage());
        } finally {
            try {
                keyOutputStream.close();
            } catch (Exception e) {
            }
            try {
                fout.close();
            } catch (Exception e) {
            }

        }
    }

    /**
     * Load the current set of tokens from the token file. If reading the tokens
     * fails or the token file does not exist, tokens will be generated on
     * demand.
     */
    private void loadTokens() {
        if (tokenFile.isFile() && tokenFile.canRead()) {
            FileInputStream fin = null;
            DataInputStream keyInputStream = null;
            try {
                fin = new FileInputStream(tokenFile);
                keyInputStream = new DataInputStream(fin);
                int newCurrentToken = keyInputStream.readInt();
                long newNextUpdate = keyInputStream.readLong();
                SecretKey[] newKeys = new SecretKey[TOKEN_BUFFER_SIZE];
                for (int i = 0; i < newKeys.length; i++) {
                    int isNull = keyInputStream.readInt();
                    if (isNull == 1) {
                        int l = keyInputStream.readInt();
                        byte[] b = new byte[l];
                        keyInputStream.read(b);
                        newKeys[i] = new SecretKeySpec(b, HMAC_SHA1);
                    } else {
                        newKeys[i] = null;
                    }
                }

                // assign the tokes and schedule a next update
                nextUpdate = newNextUpdate;
                currentToken = newCurrentToken;
                currentTokens = newKeys;

            } catch (IOException e) {

                log.error("Failed to load cookie keys " + e.getMessage());

            } finally {

                if (keyInputStream != null) {
                    try {
                        keyInputStream.close();
                    } catch (IOException e) {
                    }
                } else if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        // if there was a failure to read the current tokens, create new ones
        if (currentTokens == null) {
            currentTokens = new SecretKey[TOKEN_BUFFER_SIZE];
            nextUpdate = System.currentTimeMillis();
            currentToken = 0;
        }
    }

    /**
     * Encode a byte array.
     *
     * @param base
     * @return
     */
    private String byteToHex(byte[] base) {
        char[] c = new char[base.length * 2];
        int i = 0;

        for (byte b : base) {
            int j = b;
            j = j + 128;
            c[i++] = TOHEX[j / 0x10];
            c[i++] = TOHEX[j % 0x10];
        }
        return new String(c);
    }

    /**
     * Creates a byte array of entry from the current state of the system:
     * <ul>
     * <li>The current system time in milliseconds since the epoch</li>
     * <li>The number of nanoseconds since system startup</li>
     * <li>The name, size and last modification time of the files in the
     * <code>java.io.tmpdir</code> folder.</li>
     * </ul>
     * <p>
     * <b>NOTE</b> This method generates entropy fast but not necessarily
     * secure enough for seeding the random number generator.
     *
     * @return bytes of entropy
     */
    private static byte[] getFastEntropy() {
        final MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            throw new InternalError("internal error: SHA-1 not available.");
        }

        // update with XorShifted time values
        update(md, System.currentTimeMillis());
        update(md, System.nanoTime());

        // scan the temp file system
        File file = new File(System.getProperty("java.io.tmpdir"));
        File[] entries = file.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                md.update(entry.getName().getBytes());
                update(md, entry.lastModified());
                update(md, entry.length());
            }
        }

        return md.digest();
    }

    /**
     * Updates the message digest with an XOR-Shifted value.
     *
     * @param md The MessageDigest to update
     * @param value The original value to be XOR-Shifted first before taking the
     *            bytes ot update the message digest
     */
    private static void update(final MessageDigest md, long value) {
        value ^= (value << 21);
        value ^= (value >>> 35);
        value ^= (value << 4);

        for (int i = 0; i < 8; i++) {
            md.update((byte) value);
            value >>= 8;
        }
    }
}