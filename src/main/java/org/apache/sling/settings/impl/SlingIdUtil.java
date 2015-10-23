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
     * Read the id from a file.
     */
    static String readSlingId(final File idFile, int maxLength) {
        if (idFile.exists() && idFile.length() >= maxLength) {
            DataInputStream dis = null;
            try {
                final byte[] rawBytes = new byte[maxLength];
                dis = new DataInputStream(new FileInputStream(idFile));
                dis.readFully(rawBytes);
                final String rawString = new String(rawBytes, "ISO-8859-1");

                // roundtrip to ensure correct format of UUID value
                final String id = UUID.fromString(rawString).toString();
                // logger.debug("Got Sling ID {} from file {}", id, idFile);

                return id;
            } catch (final Throwable t) {
                // logger.error("Failed reading UUID from id file " + idFile
                //        + ", creating new id", t);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException ignore){}
                }
            }
        }
        return null;
    }

    /**
     * Write the sling id file.
     */
    static void writeSlingId(final File idFile, final String id) {
        idFile.delete();
        idFile.getParentFile().mkdirs();
        DataOutputStream dos = null;
        try {
            final byte[] rawBytes = id.getBytes("ISO-8859-1");
            dos = new DataOutputStream(new FileOutputStream(idFile));
            dos.write(rawBytes, 0, rawBytes.length);
            dos.flush();
        } catch (final Throwable t) {
            // logger.error("Failed writing UUID to id file " + idFile, t);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException ignore) {}
            }
        }
    }

}
