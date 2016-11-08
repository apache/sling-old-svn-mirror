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
package org.apache.sling.tooling.support.source.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class FelixJettySourceReferenceFinder implements SourceReferenceFinder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public List<SourceReference> findSourceReferences(Bundle bundle) throws SourceReferenceException {
        // the org.apache.felix.http.jetty bundle does not retain references to the source bundles
        // so infer them from the X-Jetty-Version header
        if ( !bundle.getSymbolicName().equals("org.apache.felix.http.jetty")) {
            return Collections.emptyList();
        }
            
        final Object jettyVersion = bundle.getHeaders().get("X-Jetty-Version");
        if ( !(jettyVersion instanceof String) ) {
            log.warn("Could not retrieve Jetty version from bundle '{}' because header 'X-Jetty-Version' is not set!", bundle);
            return Collections.emptyList();
        }
        Enumeration<URL> entries = bundle.findEntries("META-INF/maven", "pom.xml", true);
        if (entries != null && entries.hasMoreElements()) {
            URL entry = entries.nextElement();
            InputStream pom = null;
            try {
                pom = entry.openStream();
                
                try {
                    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                    SAXParser parser = parserFactory.newSAXParser();
                    PomHandler handler = new PomHandler((String) jettyVersion);
                    parser.parse(new InputSource(pom), handler);
                    
                    return handler.getReferences();
                } catch (SAXException e) {
                    throw new SourceReferenceException(e);
                } catch (ParserConfigurationException e) {
                    throw new SourceReferenceException(e);
                } finally {
                    IOUtils.closeQuietly(pom);
                }
            } catch (IOException e) {
                throw new SourceReferenceException(e);
            } finally {
                IOUtils.closeQuietly(pom);
            }
            
        } else {
            log.warn("Could not find a pom.xml in bundle '{}'!", bundle);
            return Collections.emptyList();
        }
    }

}
