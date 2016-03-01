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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.xml.sax.SAXException;

public class PomHandlerTest {

    @Test
    public void parseJettyPom() throws SAXException, IOException, ParserConfigurationException {
        
        PomHandler handler = new PomHandler("9.2.4");
        
        SAXParserFactory.newInstance().newSAXParser().parse(getClass().getResourceAsStream("felix-pom.xml"), handler);
        
        List<SourceReference> references = handler.getReferences();
        
        assertEquals(10, references.size());
        
        SourceReference jettyServlet = references.get(0);
        assertEquals("org.eclipse.jetty", jettyServlet.getGroupId());
        assertEquals("jetty-servlet", jettyServlet.getArtifactId());
        assertEquals("9.2.4", jettyServlet.getVersion());
        
        SourceReference felixApi = references.get(8);
        assertEquals("org.apache.felix", felixApi.getGroupId());
        assertEquals("org.apache.felix.http.api", felixApi.getArtifactId());
        assertEquals("3.0.0", felixApi.getVersion());
    }
}
