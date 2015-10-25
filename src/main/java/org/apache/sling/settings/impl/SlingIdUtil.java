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
package org.apache.sling.settings.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class SlingIdUtil {

    /**
     * The length in bytes of a sling identifier
     */
    private static final int SLING_ID_LENGTH = 36;

    public static String createSlingId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Read the id from a file.
     */
    public static String readSlingId(final File idFile) throws IOException {
        if (idFile.exists() && idFile.length() >= SLING_ID_LENGTH) {
            DataInputStream dis = null;
            try {
                final byte[] rawBytes = new byte[SLING_ID_LENGTH];
                dis = new DataInputStream(new FileInputStream(idFile));
                dis.readFully(rawBytes);
                final String rawString = new String(rawBytes, "ISO-8859-1");

                // roundtrip to ensure correct format of UUID value
                return UUID.fromString(rawString).toString();
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * Write the sling id file.
     */
    public static void writeSlingId(final File idFile, final String id) throws IOException {
        idFile.delete();
        idFile.getParentFile().mkdirs();
        DataOutputStream dos = null;
        try {
            final byte[] rawBytes = id.getBytes("ISO-8859-1");
            dos = new DataOutputStream(new FileOutputStream(idFile));
            dos.write(rawBytes, 0, rawBytes.length);
            dos.flush();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

}
