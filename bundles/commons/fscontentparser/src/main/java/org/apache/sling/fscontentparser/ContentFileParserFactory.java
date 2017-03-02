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
package org.apache.sling.fscontentparser;

import org.apache.sling.fscontentparser.impl.JcrXmlContentFileParser;
import org.apache.sling.fscontentparser.impl.JsonContentFileParser;

/**
 * Factory for content file parsers.
 */
public final class ContentFileParserFactory {

    private ContentFileParserFactory() {
        // static methods only
    }
    
    /**
     * Create content file parser.
     * @param type Content file type
     * @return Content file parser
     */
    public static ContentFileParser create(ContentFileType type) {
        return create(type, new ParserOptions());
    }
    
    /**
     * Create content file parser.
     * @param type Content file type
     * @param options Parser options
     * @return Content file parser
     */
    public static ContentFileParser create(ContentFileType type, ParserOptions options) {
        switch (type) {
            case JSON:
                return new JsonContentFileParser(options);
            case JCR_XML:
                return new JcrXmlContentFileParser(options);
            default:
                throw new IllegalArgumentException("Unsupported file extension: " + type);
        }
    }
    
}
