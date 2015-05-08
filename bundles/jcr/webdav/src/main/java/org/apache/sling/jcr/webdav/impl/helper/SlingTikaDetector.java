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
package org.apache.sling.jcr.webdav.impl.helper;

import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.commons.contentdetection.ContentAwareMimeTypeService;
import org.apache.sling.commons.contentdetection.FileNameExtractor;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlingTikaDetector implements Detector {

    private static final Logger log = LoggerFactory.getLogger(SlingTikaDetector.class);

    private final MimeTypeService mimeTypeService;
    private final FileNameExtractor fileNameExtractor;
    private final ContentAwareMimeTypeService contentAwareMimeTypeService;


    public SlingTikaDetector(MimeTypeService mimeTypeService,
            ContentAwareMimeTypeService contentAwareMimeTypeService,
            FileNameExtractor fileNameExtractor) {
        this.mimeTypeService = mimeTypeService;
        this.fileNameExtractor = fileNameExtractor;
        this.contentAwareMimeTypeService = contentAwareMimeTypeService;
    }

    public MediaType detect(InputStream rawData, Metadata metadata) {

        // NOTE: This implementation is built after the Tika NameDetector
        //    implementation which only takes the resource name into
        //    consideration when trying to detect the MIME type.

        // Look for a resource name in the input metadata
        String name = metadata.get(Metadata.RESOURCE_NAME_KEY);
        if (name != null) {

            name = fileNameExtractor.extract(name);

            if (name.length() > 0) {
                // Match the name against the registered patterns
                String type = null;
                if(contentAwareMimeTypeService != null) {
                    try {
                        type = contentAwareMimeTypeService.getMimeType(name, rawData);
                    } catch (IOException e) {
                        log.warn("Unable to detect mime type from content, falling back to filename based detection", e);
                        type = mimeTypeService.getMimeType(name);
                    }
                } else {
                    type = mimeTypeService.getMimeType(name);
                }

                if (type != null) {
                    return MediaType.parse(type);
                }
            }
        }

        return MediaType.OCTET_STREAM;
    }

}
