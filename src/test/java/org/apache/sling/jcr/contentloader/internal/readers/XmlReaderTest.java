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
package org.apache.sling.jcr.contentloader.internal.readers;

import junit.framework.TestCase;
import org.apache.sling.jcr.contentloader.internal.ContentCreator;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * 	Test the XmlReader with an XSLT transform
 */
public class XmlReaderTest extends TestCase {

    public void testXmlReader() throws Exception {
        XmlReader reader = new XmlReader();
        File file = new File("src/test/resources/reader/sample.xml");
        final URL testdata  = file.toURI().toURL();
        final MockContentCreator creator = new MockContentCreator();
        reader.parse(testdata, creator);
        assertEquals("Did not create expected number of nodes", 1, creator.size());
    }

    @SuppressWarnings("serial")
	private static class MockContentCreator extends ArrayList<String> implements ContentCreator {

        public MockContentCreator() {
        }

        public void createNode(String name, String primaryNodeType, String[] mixinNodeTypes) throws RepositoryException {
            this.add(name);
        }

        public void finishNode() throws RepositoryException {
        }

        public void createProperty(String name, int propertyType, String value) throws RepositoryException {
        }

        public void createProperty(String name, int propertyType, String[] values) throws RepositoryException {
        }

        public void createProperty(String name, Object value) throws RepositoryException {
        }

        public void createProperty(String name, Object[] values) throws RepositoryException {
        }

        public void createFileAndResourceNode(String name, InputStream data, String mimeType, long lastModified) throws RepositoryException {
        }

        public boolean switchCurrentNode(String subPath, String newNodeType) throws RepositoryException {
            return true;
        }
    }
}
