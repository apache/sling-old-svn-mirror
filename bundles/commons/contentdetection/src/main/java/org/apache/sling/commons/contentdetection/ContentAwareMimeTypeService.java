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
package org.apache.sling.commons.contentdetection;

import org.apache.sling.commons.mime.MimeTypeService;

import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>ContentAwareMimeTypeService</code> interface extends the
 * {@link org.apache.sling.commons.mime.MimeTypeService} API for services
 * which can detect mime types based on the content passed to them.
 * <p>
 * The implementing services should rely on analyzing the content to ascertain
 * the mime type. This interface may be implemented by bundles wishing to provide
 * a mechanism to detect mime type based on the contents.
 */

public interface ContentAwareMimeTypeService extends MimeTypeService {
    /**
     * @param filename Used if <code>content</code> is <code>null</code> or if
     *                 this service does not support content-based detection
     * @param contentStream  Optional stream that points to the content to analyze,
     *                 must support mark/reset.
     * @throws IllegalArgumentException if contentStream does not support mark/reset
     * @throws IOException if there's a problem reading the contentStream                  
     * @return the mime type
     */
    String getMimeType(String filename, InputStream contentStream) throws IOException, IllegalArgumentException;
}
