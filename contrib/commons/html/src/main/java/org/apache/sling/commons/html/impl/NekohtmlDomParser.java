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
package org.apache.sling.commons.html.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.xerces.parsers.AbstractDOMParser;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;

/**
 * DOM Parser based on the neko html parser.
 */
public class NekohtmlDomParser extends AbstractDOMParser {

    public NekohtmlDomParser(Properties properties) {
        super(getConfig(properties));
    }

    protected static HTMLConfiguration getConfig(Properties properties) {
        final HTMLConfiguration config = new HTMLConfiguration();
        config.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        if (properties != null) {
            for (Iterator<Object> i = properties.keySet().iterator(); i.hasNext();) {
                final String name = i.next().toString();
                config.setProperty(name, properties.getProperty(name));
            }
        }
        return config;
    }

    /**
     * Parse html.
     */
    public static Document parse(String systemId, InputStream stream, String encoding)
    throws IOException {
        final NekohtmlDomParser parser = new NekohtmlDomParser(null);
        XMLInputSource source = new XMLInputSource(null, systemId, null, stream, encoding);
        parser.parse(source);
        return parser.getDocument();
    }
}
