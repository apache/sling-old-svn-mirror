package org.apache.sling.ide.impl.resource.serialization;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

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
    public void emptySerializedData() throws IOException, SAXException {

        String serializationData = sm.buildSerializationData(newResourceWithProperties(new HashMap<String, Object>()), null);

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
    public void nullSerializedData() throws IOException, SAXException {

        String serializationData = sm.buildSerializationData(null, null);

        assertThat(serializationData, is(nullValue()));
    }

    @Test
    public void stringSerializedData() throws IOException, SAXException {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("jcr:createdBy", "admin");
        data.put("jcr:lastModifiedBy", "author");

        String serializationData = sm.buildSerializationData(newResourceWithProperties(data), null);

        String methodName = "stringSerializedData";

        assertXmlOutputIsEqualTo(serializationData, methodName);
    }

    private void assertXmlOutputIsEqualTo(String serializationData, String methodName) throws SAXException, IOException {

        InputStream doc = readSerializationDataFile(methodName);

        assertXMLEqual(new InputSource(doc), new InputSource(new StringReader(serializationData)));
    }

    private InputStream readSerializationDataFile(String methodName) {
        String name = getClass().getSimpleName() + "." + methodName + ".xml";
        InputStream doc = getClass().getResourceAsStream(name);
        if (doc == null)
            throw new RuntimeException("No test file found for '" + methodName + "'");
        return doc;
    }

    @Test
    public void serializedDataIsEscaped() throws IOException, SAXException {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("jcr:description", "<p class=\"active\">Welcome</p>");

        String serializationData = sm.buildSerializationData(newResourceWithProperties(data), null);

        String methodName = "serializedDataIsEscaped";

        assertXmlOutputIsEqualTo(serializationData, methodName);
    }

    @Test
    public void readSerializedData() throws IOException, SAXException {

        Map<String, Object> serializationData = sm
                .readSerializationData(readSerializationDataFile("stringSerializedData"));

        Map<String, Object> expected = new HashMap<String, Object>();
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
        
        String serializationFilePath = sm.getSerializationFilePath("jcr_root");
        
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
