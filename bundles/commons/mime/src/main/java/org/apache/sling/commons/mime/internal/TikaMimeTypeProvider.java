/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.mime.internal;

import org.apache.sling.commons.mime.MimeTypeProvider;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

/**
 * MIME type provider based on Apache Tika.
 */
public class TikaMimeTypeProvider implements MimeTypeProvider {

    private final Tika tika = new Tika();

    private final MimeTypes types = MimeTypes.getDefaultMimeTypes();

    public String getMimeType(String name) {
        String type = tika.detect(name);
        if ("application/octet-stream".equals(type)) {
            return null;
        }

        return type;
    }

    public String getExtension(String mimeType) {
        try {
            MimeType type = types.forName(mimeType);
            String extension = type.getExtension();
            if (extension != null && extension.length() > 1) {
                return extension.substring(1); // skip leading "."
            }
        } catch (MimeTypeException e) {
            // ignore
        }

        // fall back
        return null;
    }

}
