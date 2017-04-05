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

package org.apache.sling.commons.contentdetection.internal;

import org.apache.sling.commons.contentdetection.ContentAwareMimeTypeService;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.io.InputStream;

@Component(
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Content Aware MIME Type Service",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        "detection.mode=tika"
    }
)
public class ContentAwareMimeTypeServiceImpl implements  ContentAwareMimeTypeService {

    @Reference
    Detector detector;

    @Reference
    MimeTypeService mimeTypeService;

    public String getMimeType(String filename, InputStream content) throws IOException, IllegalArgumentException {
        if(content == null) {
            return mimeTypeService.getMimeType(filename);
        }
        if(!content.markSupported()) {
            throw new IllegalArgumentException("Supplied InputStream does not support mark/reset");
        }
        TikaInputStream stream = TikaInputStream.get(content);
        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, filename);
        MediaType mediaType = detector.detect(stream, metadata);
        return mediaType.toString();
    }

    public String getMimeType(String name) {
        return mimeTypeService.getMimeType(name);
    }

    public String getExtension(String mimeType) {
        return mimeTypeService.getExtension(mimeType);
    }

    public void registerMimeType(String mimeType, String... extensions) {
        mimeTypeService.registerMimeType(mimeType, extensions);
    }

    public void registerMimeType(InputStream mimeTabStream) throws IOException {
        mimeTypeService.registerMimeType(mimeTabStream);
    }
}
