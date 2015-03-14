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

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.contentloader.ContentCreator;
import org.apache.sling.jcr.contentloader.ContentReader;
import org.kxml2.io.KXmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This reader reads an xml file defining the content. The xml format should have this
 * format:
 *
 * <pre>
 * &lt;node&gt;
 *   &lt;name&gt;the name of the node&lt;/name&gt;
 *   &lt;primaryNodeType&gt;type&lt;/primaryNodeType&gt;
 *   &lt;mixinNodeTypes&gt;
 *     &lt;mixinNodeType&gt;mixtype1&lt;/mixinNodeType&gt;
 *     &lt;mixinNodeType&gt;mixtype2&lt;/mixinNodeType&gt;
 *   &lt;/mixingNodeTypes&gt;
 *   &lt;properties&gt;
 *     &lt;property&gt;
 *       &lt;name&gt;propName&lt;/name&gt;
 *       &lt;value&gt;propValue&lt;/value&gt;
 *           or
 *       &lt;values&gt;
 *         &lt;value/&gt; for multi value properties
 *       &lt;/values&gt;
 *       &lt;type&gt;propType&lt;/type&gt;
 *     &lt;/property&gt;
 *     &lt;!-- more properties --&gt;
 *   &lt;/properties&gt;
 *   &lt;nodes&gt;
 *     &lt;!-- child nodes --&gt;
 *     &lt;node&gt;
 *       ..
 *     &lt;/node&gt;
 *   &lt;/nodes&gt;
 * &lt;/node&gt;
 * </pre>
 *
 * If you want to include a binary file in your loaded content, you may specify it using a
 * {@link org.apache.sling.jcr.contentloader.internal.readers.XmlReader.FileDescription <code>&lt;nt:file&gt;</code>} element.
 */
@Component
@Service
@Properties({
    @Property(name = ContentReader.PROPERTY_EXTENSIONS, value = "xml"),
    @Property(name = ContentReader.PROPERTY_TYPES, value = {"application/xml", "text/xml"})
})
public class XmlReader implements ContentReader {

    /*
     * <node> <primaryNodeType>type</primaryNodeType> <mixinNodeTypes>
     * <mixinNodeType>mixtype1</mixinNodeType> <mixinNodeType>mixtype2</mixinNodeType>
     * </mixinNodeTypes> <properties> <property> <name>propName</name>
     * <value>propValue</value> <type>propType</type> </property> <!-- more
     * --> </properties> </node>
     */

    /** default log */
    private static final Logger logger = LoggerFactory.getLogger(XmlReader.class);

    private static final String ELEM_NODE = "node";

    private static final String ELEM_PRIMARY_NODE_TYPE = "primaryNodeType";

    private static final String ELEM_MIXIN_NODE_TYPE = "mixinNodeType";

    private static final String ELEM_PROPERTY = "property";

    private static final String ELEM_NAME = "name";

    private static final String ELEM_VALUE = "value";

    private static final String ELEM_VALUES = "values";

    private static final String ELEM_TYPE = "type";

    private static final String XML_STYLESHEET_PROCESSING_INSTRUCTION = "xml-stylesheet";

    private static final String HREF_ATTRIBUTE = "href";

    private static final String ELEM_FILE_NAMESPACE = "http://www.jcp.org/jcr/nt/1.0";
    private static final String ELEM_FILE_NAME = "file";

    private KXmlParser xmlParser;

    @Activate
    protected void activate() {
        this.xmlParser = new KXmlParser();
        try {
            // Make namespace-aware
            this.xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- XML content access -------------------------------------------


    /**
     * @see org.apache.sling.jcr.contentloader.ContentReader#parse(URL, org.apache.sling.jcr.contentloader.ContentCreator)
     */
    public synchronized void parse(final URL url, final ContentCreator creator)
    throws IOException, RepositoryException {
        BufferedInputStream bufferedInput = null;
        try {
            // We need to buffer input, so that we can reset the stream if we encounter an XSL stylesheet reference
            bufferedInput = new BufferedInputStream(url.openStream());
            parseInternal(bufferedInput, creator, url);
        } catch (XmlPullParserException xppe) {
            throw (IOException) new IOException(xppe.getMessage()).initCause(xppe);
        } finally {
            closeStream(bufferedInput);
        }
    }

    /* (non-Javadoc)
	 * @see org.apache.sling.jcr.contentloader.ContentReader#parse(java.io.InputStream, org.apache.sling.jcr.contentloader.ContentCreator)
	 */
	public void parse(InputStream ins, ContentCreator creator)
			throws IOException, RepositoryException {
        BufferedInputStream bufferedInput = null;
        try {
            // We need to buffer input, so that we can reset the stream if we encounter an XSL stylesheet reference
            bufferedInput = new BufferedInputStream(ins);
            URL xmlLocation = null;
            parseInternal(bufferedInput, creator, xmlLocation);
        } catch (XmlPullParserException xppe) {
            throw (IOException) new IOException(xppe.getMessage()).initCause(xppe);
        } finally {
            closeStream(bufferedInput);
        }
	}

	private void parseInternal(final InputStream bufferedInput,
                               final ContentCreator creator,
                               final URL xmlLocation)
    throws XmlPullParserException, IOException, RepositoryException {
        final StringBuilder contentBuffer = new StringBuilder();
        // Mark the beginning of the stream. We assume that if there's an XSL processing instruction,
        // it will occur in the first gulp - which makes sense, as processing instructions must be
        // specified before the root element of an XML file.
        bufferedInput.mark(bufferedInput.available());
        // set the parser input, use null encoding to force detection with
        // <?xml?>
        this.xmlParser.setInput(bufferedInput, null);

        NodeDescription.SHARED.clear();
        PropertyDescription.SHARED.clear();
        FileDescription.SHARED.clear();

        NodeDescription currentNode = null;
        PropertyDescription currentProperty = null;
        String currentElement;


        int eventType = this.xmlParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.PROCESSING_INSTRUCTION) {
                ProcessingInstruction pi = new ProcessingInstruction(this.xmlParser.getText());
                // Look for a reference to an XSL stylesheet
                if (pi.getName().equals(XML_STYLESHEET_PROCESSING_INSTRUCTION) && xmlLocation != null ) {
                    // Rewind the input stream to the beginning, so that it can be transformed with XSL
                    bufferedInput.reset();
                    // Pipe the XML input through the XSL transformer
                    XslTransformerStream transformerStream = new XslTransformerStream(bufferedInput, pi.getAttribute(HREF_ATTRIBUTE), xmlLocation);
                    // Start the transformer thread
                    transformerStream.startTransform();
                    // Re-run the XML parser, now with the transformed XML
                    parseInternal(transformerStream, creator, xmlLocation);
                    transformerStream.close();
                    return;

                }
            }
            if (eventType == XmlPullParser.START_TAG) {

                currentElement = this.xmlParser.getName();

                if (ELEM_PROPERTY.equals(currentElement)) {
                    currentNode = NodeDescription.create(currentNode, creator);
                    currentProperty = PropertyDescription.SHARED;
                } else if (ELEM_NODE.equals(currentElement)) {
                    currentNode = NodeDescription.create(currentNode, creator);
                    currentNode = NodeDescription.SHARED;
                } else if (ELEM_FILE_NAME.equals(currentElement) && ELEM_FILE_NAMESPACE.equals(this.xmlParser.getNamespace())) {
                    if (xmlLocation != null) {
                        int attributeCount = this.xmlParser.getAttributeCount();
                        if (attributeCount < 2 || attributeCount > 3) {
                            throw new IOException("File element must have these attributes: url, mimeType and lastModified: " + xmlLocation);
                        }
                        try {
                            AttributeMap attributes = AttributeMap.getInstance();
                            attributes.setValues(xmlParser);
                            FileDescription.SHARED.setBaseLocation(xmlLocation);
                            FileDescription.SHARED.setValues(attributes);
                            attributes.clear();
                        } catch (ParseException e) {
                            IOException ioe = new IOException("Error parsing file description: " + xmlLocation);
                            ioe.initCause(e);
                            throw ioe;
                        }
                        FileDescription.SHARED.create(creator);
                        FileDescription.SHARED.clear();
                    } else {
                        logger.warn("file element encountered when xml location isn't known. skipping.");
                    }
                }

            } else if (eventType == XmlPullParser.END_TAG) {

                String qName = this.xmlParser.getName();
                String content = contentBuffer.toString().trim();
                contentBuffer.delete(0, contentBuffer.length());

                if (ELEM_PROPERTY.equals(qName)) {
                    currentProperty = PropertyDescription.create(currentProperty, creator);

                } else if (ELEM_NAME.equals(qName)) {
                    if (currentProperty != null) {
                        currentProperty.name = content;
                    } else if (currentNode != null) {
                        currentNode.name = content;
                    }

                } else if (ELEM_VALUE.equals(qName)) {
                    if ( currentProperty == null ) {
                        throw new IOException("XML file does not seem to contain valid content xml. Unexpected " + ELEM_VALUE + " element in : " + xmlLocation);
                    }
                    currentProperty.addValue(content);

                } else if (ELEM_VALUES.equals(qName)) {
                    if ( currentProperty == null ) {
                        throw new IOException("XML file does not seem to contain valid content xml. Unexpected " + ELEM_VALUE + " element in : " + xmlLocation);
                    }
                    currentProperty.isMultiValue = true;

                } else if (ELEM_TYPE.equals(qName)) {
                    if ( currentProperty == null ) {
                        throw new IOException("XML file does not seem to contain valid content xml. Unexpected " + ELEM_VALUE + " element in : " + xmlLocation);
                    }
                    currentProperty.type = content;

                } else if (ELEM_NODE.equals(qName)) {
                    currentNode = NodeDescription.create(currentNode, creator);
                    creator.finishNode();

                } else if (ELEM_PRIMARY_NODE_TYPE.equals(qName)) {
                    if ( currentNode == null ) {
                        throw new IOException("Element is not allowed at this location: " + qName + " in " + xmlLocation);
                    }
                    currentNode.primaryNodeType = content;

                } else if (ELEM_MIXIN_NODE_TYPE.equals(qName)) {
                    if ( currentNode == null ) {
                        throw new IOException("Element is not allowed at this location: " + qName + " in " + xmlLocation);
                    }
                    currentNode.addMixinType(content);
                }
            } else if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.CDSECT) {
                contentBuffer.append(this.xmlParser.getText());
            }

            eventType = this.xmlParser.nextToken();
        }
    }

    /**
     * Takes an XML input stream and pipes it through an XSL transformer.
     * Callers should call {@link #startTransform} before trying to use the stream, or the caller will wait indefinately for input.
     */
    private static class XslTransformerStream extends PipedInputStream {
        private InputStream inputXml;
        private String xslHref;
        private Thread transformerThread;
        private PipedOutputStream pipedOut;
        private URL xmlLocation;

        /**
         * Instantiate the XslTransformerStream.
         * @param inputXml XML to be transformed.
         * @param xslHref Path to an XSL stylesheet
         * @param xmlLocation
         * @throws IOException
         */
        public XslTransformerStream(InputStream inputXml, String xslHref, URL xmlLocation) throws IOException {
            super();
            this.inputXml = inputXml;
            this.xslHref = xslHref;
            this.transformerThread = null;
            this.pipedOut = new PipedOutputStream(this);
            this.xmlLocation = xmlLocation;
        }

        /**
         * Starts the XSL transformer in a new thread, so that it can pipe its output to our <code>PipedInputStream</code>.
         * @throws IOException
         */
        public void startTransform() throws IOException {
            final URL xslResource = new URL(xmlLocation, this.xslHref);

/*
            if (xslResource == null) {
                throw new IOException("Could not find " + xslHref);
            }
*/

            transformerThread = new Thread(
                    new Runnable() {
                        public void run() {
                            try {
                                Source xml = new StreamSource(inputXml);
                                Source xsl = new StreamSource(xslResource.toExternalForm());
                                final StreamResult streamResult;
                                final Templates templates = TransformerFactory.newInstance().newTemplates(xsl);
                                streamResult = new StreamResult(pipedOut);
                                templates.newTransformer().transform(xml, streamResult);
                            } catch (TransformerConfigurationException e) {
                                throw new RuntimeException("Error initializing XSL transformer", e);
                            } catch (TransformerException e) {
                                throw new RuntimeException("Error transforming", e);
                            } finally {
                                closeStream(pipedOut);
                            }
                        }
                    }
                    , "XslTransformerThread");
            transformerThread.start();
        }


    }

    /**
     * Utility function to close a stream if it is still open.
     * @param closeable Stream to close
     */
    private static void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }

    protected static final class NodeDescription {

        public static NodeDescription SHARED = new NodeDescription();

        public String name;
        public String primaryNodeType;
        public List<String> mixinTypes;

        public static NodeDescription create(NodeDescription desc, ContentCreator creator)
                throws RepositoryException {
            if ( desc != null ) {
                creator.createNode(desc.name, desc.primaryNodeType, desc.getMixinTypes());
                desc.clear();
            }
            return null;
        }

        public void addMixinType(String v) {
            if ( this.mixinTypes == null ) {
                this.mixinTypes = new ArrayList<String>();
            }
            this.mixinTypes.add(v);
        }


        private String[] getMixinTypes() {
            if ( this.mixinTypes == null || this.mixinTypes.size() == 0) {
                return null;
            }
            return mixinTypes.toArray(new String[this.mixinTypes.size()]);
        }

        private void clear() {
            this.name = null;
            this.primaryNodeType = null;
            if ( this.mixinTypes != null ) {
                this.mixinTypes.clear();
            }
        }
    }

    protected static final class PropertyDescription {

        public static PropertyDescription SHARED = new PropertyDescription();

        public static PropertyDescription create(PropertyDescription desc, ContentCreator creator)
                throws RepositoryException {
            int type = (desc.type == null ? PropertyType.STRING : PropertyType.valueFromName(desc.type));
            if ( desc.isMultiValue ) {
                creator.createProperty(desc.name, type, desc.getPropertyValues());
            } else {
                String value = null;
                if ( desc.values != null && desc.values.size() == 1 ) {
                    value = desc.values.get(0);
                }
                creator.createProperty(desc.name, type, value);
            }
            desc.clear();
            return null;
        }

        public String name;
        public String type;
        public List<String> values;
        public boolean isMultiValue;

        public void addValue(String v) {
            if ( this.values == null ) {
                this.values = new ArrayList<String>();
            }
            this.values.add(v);
        }

        private String[] getPropertyValues() {
            if ( this.values == null || this.values.size() == 0) {
                return null;
            }
            return values.toArray(new String[this.values.size()]);
        }

        private void clear() {
            this.name = null;
            this.type = null;
            if ( this.values != null ) {
                this.values.clear();
            }
            this.isMultiValue = false;
        }
    }

    /**
     * Represents an XML processing instruction.<br />
     * A processing instruction like <code>&lt;?xml-stylesheet href="stylesheet.xsl" type="text/css"?&gt</code>
     * will have <code>name</code> == <code>"xml-stylesheet"</code> and two attributes: <code>href</code> and <code>type</code>.
     */
    private static class ProcessingInstruction {

        private Map<String, String> attributes = new HashMap<String, String>();
        private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\s(.[^=\\s]*)\\s?=\\s?\"(.[^\"]*)\"");
        private static final Pattern NAME_PATTERN = Pattern.compile("^(.[^\\s\\?>]*)");
        private String name;

        public ProcessingInstruction(String text) throws IOException {
            final Matcher nameMatcher = NAME_PATTERN.matcher(text);
            if (!nameMatcher.find()) {
                throw new IOException("Malformed processing instruction: " + text);
            }

            this.name = nameMatcher.group(1);
            final Matcher attributeMatcher = ATTRIBUTE_PATTERN.matcher(text);
            while (attributeMatcher.find()) {
                attributes.put(attributeMatcher.group(1), attributeMatcher.group(2));
            }
        }

        public String getName() {
            return name;
        }

        public String getAttribute(String key) {
            return this.attributes.get(key);
        }

    }

    /**
     * Represents a reference to a file that is to be loaded into the repository. The file is referenced by an
     * XML element named <code>&lt;nt:file&gt;</code>, with the attributes <code>src</code>,
     * <code>mimeType</code> and <code>lastModified</code>. <br/><br/>Example:
     * <pre>
     * &lt;nt:file src="../../image.png" mimeType="image/png" lastModified="1977-06-01T07:00:00+0100" /&gt;
     * </pre>
     * The date format for <code>lastModified</code> is <code>yyyy-MM-dd'T'HH:mm:ssZ</code>.
     * The <code>lastModified</code> attribute is optional. If missing, the last modified date reported by the
     * filesystem will be used.
     */
    protected static final class FileDescription {

        private URL url;
        private String mimeType;
        private URL baseLocation;
        private Long lastModified;

        public static FileDescription SHARED = new FileDescription();
        private static final String SRC_ATTRIBUTE = "src";
        private static final String MIME_TYPE_ATTRIBUTE = "mimeType";
        private static final String LAST_MODIFIED_ATTRIBUTE = "lastModified";
        public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        static {
            DATE_FORMAT.setLenient(true);
        }

        public void setValues(AttributeMap attributes) throws MalformedURLException, ParseException {
            Set<String> attributeNames = attributes.keySet();
            for (String name : attributeNames) {
                String value = attributes.get(name);
                if (name.equals(SRC_ATTRIBUTE)) {
                    url = new URL(baseLocation, value);
                } else if (name.equals(MIME_TYPE_ATTRIBUTE)) {
                    mimeType = value;
                } else if (name.equals(LAST_MODIFIED_ATTRIBUTE)) {
                    lastModified = DATE_FORMAT.parse(value).getTime();
                }
            }
        }

        public void create(ContentCreator creator) throws RepositoryException, IOException {
            String[] parts = url.getPath().split("/");
            String name = parts[parts.length - 1];
            InputStream stream = url.openStream();
            if (lastModified == null) {
                try {
                    lastModified = new File(url.toURI()).lastModified();
                } catch (Throwable ignore) {
                    // Could not get lastModified from file system, so we'll use current date
                    lastModified = Calendar.getInstance().getTimeInMillis();
                }
            }
            creator.createFileAndResourceNode(name, stream, mimeType, lastModified);
            closeStream(stream);
            creator.finishNode();
            creator.finishNode();
            this.clear();
        }

        public URL getUrl() {
            return url;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Long getLastModified() {
            return lastModified;
        }

        public void clear() {
            this.url = null;
            this.mimeType = null;
            this.lastModified = null;
        }

        public void setBaseLocation(URL xmlLocation) {
            this.baseLocation = xmlLocation;
        }
    }

    /**
     * Utility class for dealing with attributes from KXmlParser.
     */
    protected static class AttributeMap extends HashMap<String, String> {

		private static final long serialVersionUID = -6304058237706001104L;
		private static final AttributeMap instance = new AttributeMap();

        public static AttributeMap getInstance() {
            return instance;
        }

        /**
         * Puts values in an <code>AttributeMap</code> by extracting attributes from the <code>xmlParser</code>.
         * @param xmlParser <code>xmlParser</code> to extract attributes from. The parser must be
         * in {@link org.xmlpull.v1.XmlPullParser#START_TAG} state.
         */
        public void setValues(KXmlParser xmlParser) {
            final int count = xmlParser.getAttributeCount();
            for (int i = 0; i < count; i++) {
                this.put(xmlParser.getAttributeName(i), xmlParser.getAttributeValue(i));
            }
        }
    }
}
