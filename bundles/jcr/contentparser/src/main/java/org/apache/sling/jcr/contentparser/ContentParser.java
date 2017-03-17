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
package org.apache.sling.jcr.contentparser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses repository content from a file.
 * Implementations have to be thread-safe.
 */
public interface ContentParser {

    /**
     * Parse content in a "stream-based" way. Each resource that is found in the content is reported to the contentHandler.
     * @param contentHandler Content handler that accepts the parsed content.
     * @param inputStream Stream with serialized content
     * @throws IOException When I/O error occurs.
     * @throws ParseException When parsing error occurs.
     */
    void parse(ContentHandler contentHandler, InputStream inputStream) throws IOException, ParseException;

}
