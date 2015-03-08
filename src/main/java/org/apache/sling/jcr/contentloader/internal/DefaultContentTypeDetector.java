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
package org.apache.sling.jcr.contentloader.internal;

import java.io.InputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.contentloader.ContentTypeDetector;
import org.apache.sling.jcr.contentloader.ContentTypeUtil;
import org.osgi.framework.Constants;

@Component(
    immediate = true
)
@Service
@Properties({
    @Property(
        name = Constants.SERVICE_VENDOR,
        value = "The Apache Software Foundation"
    ),
    @Property(
        name = Constants.SERVICE_DESCRIPTION,
        value = "Apache Sling Content Type Detector"
    )
})
public class DefaultContentTypeDetector implements ContentTypeDetector {

    /**
     * @param contentStream
     * @param filename
     * @return the detected content type or null
     */
    public String detectContentType(final InputStream contentStream, final String filename) {
        return ContentTypeUtil.detectContentType(filename);
    }

}
