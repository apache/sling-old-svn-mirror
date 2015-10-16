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
package org.apache.sling.ide.impl.resource.serialization;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.ide.serialization.SerializationData;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.transport.ResourceProxy;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SimpleXmlSerializationManagerTest {

    private SimpleXmlSerializationManager sm;

    @BeforeClass
    public static void configureXmlUnit() {
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Before
    public void prepare() {
        sm = new SimpleXmlSerializationManager();
    }

    @Test
    public void emptySerializedData() throws SerializationException, SAXException {

        SerializationData serializationData = sm.newBuilder(null, null).buildSerializationData(null,
                newResourceWithProperties(new HashMap<String, Object>()));

        assertThat(serializationData, is(nullValue()));
    }

    private ResourceProxy newResourceWithProperties(Map<String, Object> properties) {
        ResourceProxy resource = new ResourceProxy("/");
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            resource.addProperty(entry.getKey(), entry.getValue());
        }
        return resource;
    }

    @Test
    public void nullSerializedData() throws SerializationException, SAXException {

        SerializationData serializationData = sm.newBuilder(null, null).buildSerializationData(null, null);

        assertThat(serializationData, is(nullValue()));
    }

    @Test
    public void stringSerializedData() throws SerializationException, SAXException, IOException {

        Map<String, Object> data = new HashMap<>();
        data.put("jcr:createdBy", "admin");
        data.put("jcr:lastModifiedBy", "author");

        SerializationData serializationData = sm.newBuilder(null, null).buildSerializationData(null, newResourceWithProperties(data));

        String methodName = "stringSerializedData";

        assertXmlOutputIsEqualTo(serializationData.getContents(), methodName);
    }

    private void assertXmlOutputIsEqualTo(byte[] serializationData, String methodName) throws SAXException,
            SerializationException, IOException {

        InputStream doc = readSerializationDataFile(methodName);

        assertXMLEqual(new InputSource(doc), new InputSource(new ByteArrayInputStream(serializationData)));
    }

    private InputStream readSerializationDataFile(String methodName) {
        String name = getClass().getSimpleName() + "." + methodName + ".xml";
        InputStream doc = getClass().getResourceAsStream(name);
        if (doc == null)
            throw new RuntimeException("No test file found for '" + methodName + "'");
        return doc;
    }

    @Test
    public void serializedDataIsEscaped() throws SerializationException, SAXException, IOException {

        Map<String, Object> data = new HashMap<>();
        data.put("jcr:description", "<p class=\"active\">Welcome</p>");

        SerializationData serializationData = sm.newBuilder(null, null).buildSerializationData(null, newResourceWithProperties(data));

        String methodName = "serializedDataIsEscaped";

        assertXmlOutputIsEqualTo(serializationData.getContents(), methodName);
    }

    @Test
    public void readSerializedData() throws IOException, SAXException {

        Map<String, Object> serializationData = sm
                .readSerializationData(null, readSerializationDataFile("stringSerializedData")).getProperties();

        Map<String, Object> expected = new HashMap<>();
        expected.put("jcr:createdBy", "admin");
        expected.put("jcr:lastModifiedBy", "author");

        assertThat(serializationData, is(expected));
    }

    @Test
    public void serializationFileMatches() {

        assertThat(sm.isSerializationFile(".content.xml"), is(true));

    }

    @Test
    public void serializationFileDoesNotMatch() {

        assertThat(sm.isSerializationFile("content.json"), is(false));

    }

    @Test
    public void serializationFileLocation() {
        
        String serializationFilePath = sm.getSerializationFilePath("jcr_root", SerializationKind.FOLDER);
        
        assertThat(serializationFilePath, is("jcr_root" + File.separatorChar + ".content.xml"));
    }

    @Test
    public void baseResourcePath() {

        String basePath = sm.getBaseResourcePath("jcr_root" + File.separatorChar + ".content.xml");
        assertThat(basePath, is("jcr_root"));
    }

    @Test
    public void baseResourcePathIsEmpty() {
        
        String basePath = sm.getBaseResourcePath(".content.xml");
        assertThat(basePath, is(""));
    }
    
}
