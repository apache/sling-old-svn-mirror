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
package org.apache.sling.distribution.util.impl;

import static java.lang.Integer.toHexString;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class DigestUtils {

    private DigestUtils() {
        // do nothing
    }

    public static DigestInputStream openDigestInputStream(InputStream mainInputStream, String digestAlgorithm) {
        return new DigestInputStream(mainInputStream, getDigest(digestAlgorithm));
    }

    public static DigestOutputStream openDigestOutputStream(OutputStream mainOutputStream, String digestAlgorithm) {
        return new DigestOutputStream(mainOutputStream, getDigest(digestAlgorithm));
    }

    private static MessageDigest getDigest(String digestAlgorithm) {
        try {
            return MessageDigest.getInstance(digestAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Digest algorithm " + digestAlgorithm + " not supported in this version.");
        }
    }

    public static String readDigestMessage(DigestInputStream input) {
        return readDigestMessage(input.getMessageDigest());
    }

    public static String readDigestMessage(DigestOutputStream output) {
        return readDigestMessage(output.getMessageDigest());
    }

    private static String readDigestMessage(MessageDigest messageDigest) {
        StringWriter writer = new StringWriter();
        try {
            readDigest(messageDigest, writer);
        } catch (IOException e) {
            // it does not happen with the StringWriter
        } finally {
            closeQuietly(writer);
        }
        return writer.toString();
    }

    public static File rewriteDigestMessage(DigestOutputStream digestOutput, File target) throws IOException {
        File targetDigest = new File(target.getParentFile(),
                                     target.getName()
                                     + '.'
                                     + digestOutput.getMessageDigest()
                                                   .getAlgorithm()
                                                   .toLowerCase()
                                                   .replace('-', Character.MIN_VALUE));
        rewriteDigestMessage(digestOutput, new FileOutputStream(targetDigest));
        return targetDigest;
    }

    public static void rewriteDigestMessage(DigestOutputStream digestOutput, OutputStream target) throws IOException {
        final Writer writer = new OutputStreamWriter(target);
        try {
            readDigest(digestOutput.getMessageDigest(), writer);
        } finally {
            closeQuietly(writer);
            closeQuietly(target);
        }
    }

    private static void readDigest(MessageDigest messageDigest, Writer writer) throws IOException {
        final byte[] data = messageDigest.digest();
        for (byte element: data) {
            int intVal = element & 0xff;
            if (intVal < 0x10){
                // append a zero before a one digit hex
                // number to make it two digits.
                writer.append('0');
            }
            writer.append(toHexString(intVal));
        }
    }

}