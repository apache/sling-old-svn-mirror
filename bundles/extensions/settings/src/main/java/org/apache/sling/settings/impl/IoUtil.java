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

import java.io.IOException;
import java.io.InputStream;

/**
 * Ad-hoc utility class for dealing with IO. An ad-hoc class
 * is favored to a 3rd party library in this bundle in order
 * to avoid dependencies and thus be activated early.
 */
public class IoUtil {

    /**
     * value to indicates the end of a stream
     */
    private static final int EO = -1;

    /**
     * Fill a byte array with bytes read from an input stream.
     * This method blocks until the buffer is filled or the end of the input stream has been reached.
     *
     * @param is the input stream to read the bytes from
     * @param buffer the buffer to be filled
     * @return the number of bytes read
     *
     * @throws IOException if an I/O error occurs
     */
    static int fill(InputStream is, byte[] buffer) throws IOException {
        int max = buffer.length;
        int total = 0;
        for (;;) {
            int read = is.read(buffer, total, max - total);
            if (read != EO) {
                total += read;
            }
            if (read == EO || total == max) {
                return total;
            }
        }
    }
}
