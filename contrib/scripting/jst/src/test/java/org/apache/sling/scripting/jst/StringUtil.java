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
package org.apache.sling.scripting.jst;

import java.io.IOException;
import java.io.InputStream;

/** Test utilities */
class StringUtil {

    /** Read given Class resource */
    static String readClassResource(Class<?> clazz, String path, String encoding) throws IOException {
        final InputStream s = clazz.getResourceAsStream(path);
        if(s == null) {
            throw new IOException("Class resource " + path + " not found");
        }
        final byte [] buffer = new byte[4096];
        final StringBuffer result = new StringBuffer();
        int bytesRead = 0;
        while( (bytesRead = s.read(buffer, 0, buffer.length)) > 0) {
            final String str = new String(buffer, 0, bytesRead, encoding);
            result.append(str);
        }
        return result.toString();
    }
}
