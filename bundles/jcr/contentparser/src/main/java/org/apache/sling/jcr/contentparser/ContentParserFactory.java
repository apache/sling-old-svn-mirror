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

import org.apache.sling.jcr.contentparser.impl.JcrXmlContentParser;
import org.apache.sling.jcr.contentparser.impl.JsonContentParser;

/**
 * Factory for content parsers.
 */
public final class ContentParserFactory {

    private ContentParserFactory() {
        // static methods only
    }
    
    /**
     * Create content parser.
     * @param type Content type
     * @return Content parser
     */
    public static ContentParser create(ContentType type) {
        return create(type, new ParserOptions());
    }
    
    /**
     * Create content parser.
     * @param type Content type
     * @param options Parser options
     * @return Content parser
     */
    public static ContentParser create(ContentType type, ParserOptions options) {
        switch (type) {
            case JSON:
                return new JsonContentParser(options);
            case JCR_XML:
                return new JcrXmlContentParser(options);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
    
}
