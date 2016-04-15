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
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class FelixJettySourceReferenceFinder implements SourceReferenceFinder {

    @Override
    public List<SourceReference> findSourceReferences(Bundle bundle) throws SourceReferenceException {
        // the org.apache.felix.http.jetty bundle does not retain references to the source bundles
        // so infer them from the X-Jetty-Version header
        if ( !bundle.getSymbolicName().equals("org.apache.felix.http.jetty")) {
            return Collections.emptyList();
        }
            
        final Object jettyVersion = bundle.getHeaders().get("X-Jetty-Version");
        if ( !(jettyVersion instanceof String) ) {
            return Collections.emptyList();
        }
        
        URL entry = bundle.getEntry("/META-INF/maven/org.apache.felix/org.apache.felix.http.jetty/pom.xml");
        
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

    }

}
