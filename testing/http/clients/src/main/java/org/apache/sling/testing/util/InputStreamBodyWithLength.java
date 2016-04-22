/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.util;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.sling.testing.itframework.ClientException;

import java.io.IOException;
import java.io.InputStream;

/**
 * If we want to upload a file that is a resource in a jar file, the http client expects a content length.
 */
public class InputStreamBodyWithLength extends InputStreamBody {
    private long streamLength;

    public InputStreamBodyWithLength(String resourcePath, String contentType, String fileName) throws ClientException {
        super(ResourceUtil.getResourceAsStream(resourcePath), ContentType.create(contentType), fileName);
        this.streamLength = getResourceStreamLength(resourcePath);
    }

    @Override
    public long getContentLength() {
        return streamLength;
    }

    /**
     * Returns the length of a resource (which is needed for the InputStreamBody
     * to work. Can't currently think of a better solution than going through
     * the resource stream and count.
     *
     * @param resourcePath path to the file
     * @return the size of the resource
     */
    private static long getResourceStreamLength(String resourcePath) throws ClientException {
        int streamLength = 0;
        InputStream stream = ResourceUtil.getResourceAsStream(resourcePath);
        try {
            for (int avail = stream.available(); avail > 0; avail = stream.available()) {
                streamLength += avail;
                stream.skip(avail);
            }
        } catch (IOException e) {
            throw new ClientException("Could not read " + resourcePath + "!", e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                throw new ClientException("Could not close Inputstream for " + resourcePath + "!", e);
            }
        }
        return streamLength;
    }
}