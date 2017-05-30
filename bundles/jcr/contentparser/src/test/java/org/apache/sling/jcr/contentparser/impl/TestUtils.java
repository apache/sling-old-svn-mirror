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
package org.apache.sling.jcr.contentparser.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.jcr.contentparser.ContentParser;
import org.apache.sling.jcr.contentparser.impl.mapsupport.ContentElement;
import org.apache.sling.jcr.contentparser.impl.mapsupport.ContentElementHandler;

public final class TestUtils {
    
    private TestUtils() {
        // static methods only
    }

    public static ContentElement parse(ContentParser contentParser, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis)) {
            ContentElementHandler handler = new ContentElementHandler();
            contentParser.parse(handler, bis);
            return handler.getRoot();
        }
    }
    
    public static ContentElement parse(ContentParser contentParser, String jsonContent) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(jsonContent.getBytes(CharEncoding.UTF_8))) {
            ContentElementHandler handler = new ContentElementHandler();
            contentParser.parse(handler, is);
            return handler.getRoot();
        }
    }
    
}
