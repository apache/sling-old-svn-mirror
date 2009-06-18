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
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.apache.sling.jcr.contentloader.internal.ContentCreator;
import org.apache.sling.jcr.contentloader.internal.ContentReader;
import org.apache.sling.jcr.contentloader.internal.ImportProvider;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This reader reads an xml file defining the content.
 * The xml format should have this format:
 * <node>
 *   <name>the name of the node</name>
 *   <primaryNodeType>type</primaryNodeType>
 *   <mixinNodeTypes>
 *     <mixinNodeType>mixtype1</mixinNodeType>
 *     <mixinNodeType>mixtype2</mixinNodeType>
 *   </mixingNodeTypes>
 *   <properties>
 *     <property>
 *       <name>propName</name>
 *       <value>propValue</value>
 *           or
 *       <values>
 *         <value/> for multi value properties
 *       </values>
 *       <type>propType</type>
 *     </property>
 *     <!-- more properties -->
 *   </properties>
 *   <nodes>
 *     <!-- child nodes -->
 *     <node>
 *       ..
 *     </node>
 *   </nodes>
 * </node>
 */
public class XmlReader implements ContentReader {

    /*
     * <node> <primaryNodeType>type</primaryNodeType> <mixinNodeTypes>
     * <mixinNodeType>mixtype1</mixinNodeType> <mixinNodeType>mixtype2</mixinNodeType>
     * </mixinNodeTypes> <properties> <property> <name>propName</name>
     * <value>propValue</value> <type>propType</type> </property> <!-- more
     * --> </properties> </node>
     */

    /** default log */
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

    public static final ImportProvider PROVIDER = new ImportProvider() {
        private XmlReader xmlReader;

        public ContentReader getReader() throws IOException {
            if (xmlReader == null) {
                try {
                    xmlReader = new XmlReader();
                } catch (Throwable t) {
                    throw (IOException) new IOException(t.getMessage()).initCause(t);
                }
            }
            return xmlReader;
        }
    };
    private KXmlParser xmlParser;

    XmlReader() {
        this.xmlParser = new KXmlParser();
    }

    // ---------- XML content access -------------------------------------------


    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentReader#parse(java.net.URL, org.apache.sling.jcr.contentloader.internal.ContentCreator)
     */
    public synchronized void parse(java.net.URL url, ContentCreator creator)
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

    private void parseInternal(InputStream bufferedInput, ContentCreator creator, java.net.URL xmlLocation) throws XmlPullParserException, IOException, RepositoryException {
        final StringBuffer contentBuffer = new StringBuffer();
        // Mark the beginning of the stream. We assume that if there's an XSL processing instruction,
        // it will occur in the first gulp - which makes sense, as processing instructions must be
        // specified before the root elemeent of an XML file.
        bufferedInput.mark(bufferedInput.available());
        // set the parser input, use null encoding to force detection with
        // <?xml?>
        this.xmlParser.setInput(bufferedInput, null);

        NodeDescription.SHARED.clear();
        PropertyDescription.SHARED.clear();

        NodeDescription currentNode = null;
        PropertyDescription currentProperty = null;
        String currentElement;


        int eventType = this.xmlParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.PROCESSING_INSTRUCTION) {
                ProcessingInstruction pi = new ProcessingInstruction(this.xmlParser.getText());
                // Look for a reference to an XSL stylesheet
                if (pi.getName().equals(XML_STYLESHEET_PROCESSING_INSTRUCTION)) {
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
                    currentProperty.addValue(content);

                } else if (ELEM_VALUES.equals(qName)) {
                    currentProperty.isMultiValue = true;

                } else if (ELEM_TYPE.equals(qName)) {
                    currentProperty.type = content;

                } else if (ELEM_NODE.equals(qName)) {
                    currentNode = NodeDescription.create(currentNode, creator);
                    creator.finishNode();

                } else if (ELEM_PRIMARY_NODE_TYPE.equals(qName)) {
                    if ( currentNode == null ) {
                        throw new IOException("Element is not allowed at this location: " + qName);
                    }
                    currentNode.primaryNodeType = content;

                } else if (ELEM_MIXIN_NODE_TYPE.equals(qName)) {
                    if ( currentNode == null ) {
                        throw new IOException("Element is not allowed at this location: " + qName);
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
            final URL xslResource = new java.net.URL(xmlLocation, this.xslHref);

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
}
