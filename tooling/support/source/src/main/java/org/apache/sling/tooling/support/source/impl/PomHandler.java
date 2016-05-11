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

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

final class PomHandler extends DefaultHandler {
    private final String jettyVersion;
    
    // state
    private Deque<String> elements = new LinkedList<String>();
    private String artifactId;
    private String groupId;
    private String version;
    
    // output
    private List<SourceReference> references = new ArrayList<SourceReference>();

    PomHandler(String jettyVersion) {
        this.jettyVersion = jettyVersion;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        elements.add(qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        
        // project/dependencies/dependency/${tag}
        if ( elements.size() != 4 ) {
            return;
        }
        
        String tagName = elements.peekLast();
        
        if ( tagName.equals("artifactId")) {
            artifactId = new String(ch, start, length); 
        } else if ( tagName.equals("groupId")) {
            groupId = new String(ch, start, length);
        } else if ( tagName.equals("version")) {
            version = new String(ch, start, length);
        }
            
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        // project/dependencies/dependency
        if ( elements.size() == 3 && qName.equals("dependency")) {
            if ( groupId.startsWith("org.eclipse.jetty")) {
                references.add(new SourceReference(groupId, artifactId, jettyVersion));
            } else if ( groupId.startsWith("org.apache.felix")) {
                references.add(new SourceReference(groupId, artifactId, version));
            }
            
            artifactId = null;
            groupId = null;
            version = null;
        }

        
        elements.removeLast();
    }
    
    public List<SourceReference> getReferences() {
        return references;
    }
}