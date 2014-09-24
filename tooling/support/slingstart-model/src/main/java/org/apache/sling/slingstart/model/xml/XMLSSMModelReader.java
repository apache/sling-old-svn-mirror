/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.slingstart.model.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.slingstart.model.SSMArtifact;
import org.apache.sling.slingstart.model.SSMConfiguration;
import org.apache.sling.slingstart.model.SSMDeliverable;
import org.apache.sling.slingstart.model.SSMFeature;
import org.apache.sling.slingstart.model.SSMSettings;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Simple XML parser for the model.
 * It ignores all elements in a different namespace than the root element
 */
public class XMLSSMModelReader {

    public enum MODE {
        INIT(null, null),
        DELIVERABLE(INIT, "deliverable"),

        PROPERTIES(DELIVERABLE, "properties"),

        STARTLEVEL(DELIVERABLE, "startLevel"),
        ARTIFACT(DELIVERABLE, "artifact"),

        STARTLEVEL_ARTIFACT(STARTLEVEL, "artifact"),

        CONFIGURATION(DELIVERABLE, "configuration"),
        SETTINGS(DELIVERABLE, "settings"),

        FEATURE(DELIVERABLE, "feature"),
        FEATURE_STARTLEVEL(FEATURE, "startLevel"),
        FEATURE_ARTIFACT(FEATURE, "artifact"),
        FEATURE_STARTLEVEL_ARTIFACT(FEATURE_STARTLEVEL, "artifact"),

        FEATURE_CONFIGURATION(FEATURE, "configuration"),
        FEATURE_SETTINGS(FEATURE, "settings");

        public final MODE fromMode;
        public final String elementName;

        MODE(final MODE fm, final String en) {
            this.fromMode = fm;
            this.elementName = en;
        }
    }

    /**
     * Reads the deliverable file
     * The reader is not closed.
     * @throws IOException
     */
    public static SSMDeliverable read(final Reader reader)
    throws IOException {
        try {
            final SSMDeliverable result = new SSMDeliverable();

            final SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setContentHandler(new ContentHandler() {

                private final Stack<String> elementsStack = new Stack<String>();

                /** The current parsing mode. */
                private MODE mode = MODE.INIT;

                /** String builder to get text from the document. */
                private StringBuilder text;

                /** The namespace for the read xml elements. */
                private String namespace;

                /** The current feature */
                private SSMFeature feature;

                /** Current startlevel */
                private int startLevel;

                /** Current configuration. */
                private SSMConfiguration configuration;

                @Override
                public void startElement(final String uri,
                        final String localName,
                        final String qName,
                        final Attributes atts)
                throws SAXException {
                    if ( this.mode == MODE.INIT ) {
                        if ( MODE.DELIVERABLE.elementName.equals(localName) ) {
                            this.namespace = uri;
                            this.mode = MODE.DELIVERABLE;
                            this.feature = result.getOrCreateFeature(null);
                            this.startLevel = 0;
                        } else {
                            throw new SAXException("Unknown root element (" + localName + "). Document must start with " + MODE.DELIVERABLE.elementName);
                        }
                    } else {
                        if ( (uri == null && this.namespace == null) || (uri != null && uri.equals(this.namespace)) ) {
                            boolean found = false;
                            for(final MODE m : MODE.values()) {
                                if ( this.mode == m.fromMode && localName.equals(m.elementName) ) {
                                    this.mode = m;
                                    found = true;
                                    break;
                                }
                            }
                            if ( !found ) {
                                throw new SAXException("Unknown element " + localName);
                            }

                            if ( this.mode == MODE.STARTLEVEL || this.mode == MODE.FEATURE_STARTLEVEL) {
                                int level = 0;
                                final String levelVal = atts.getValue("level");
                                if ( levelVal != null ) {
                                    level = Integer.valueOf(levelVal);
                                }
                                this.startLevel = level;
                            } else if ( this.mode == MODE.ARTIFACT || this.mode == MODE.FEATURE_ARTIFACT || this.mode == MODE.STARTLEVEL_ARTIFACT || this.mode == MODE.FEATURE_STARTLEVEL_ARTIFACT) {
                                final SSMArtifact artifact = new SSMArtifact();
                                this.feature.getOrCreateStartLevel(this.startLevel).artifacts.add(artifact);
                                artifact.groupId = atts.getValue("groupId");
                                artifact.artifactId = atts.getValue("artifactId");
                                artifact.version = atts.getValue("version");
                                artifact.type = atts.getValue("type");
                                artifact.classifier = atts.getValue("classifier");
                            } else if ( this.mode == MODE.CONFIGURATION || this.mode == MODE.FEATURE_CONFIGURATION) {
                                this.configuration = this.feature.getOrCreateConfiguration(atts.getValue("pid"), atts.getValue("factory"));
                                this.text = new StringBuilder();
                            } else if ( this.mode == MODE.SETTINGS || this.mode == MODE.FEATURE_SETTINGS) {
                                if ( this.feature.getSettings() != null ) {
                                    throw new SAXException("Duplicate settings section");
                                }
                                this.feature.setSettings(new SSMSettings());
                                this.text = new StringBuilder();

                            } else if ( this.mode == MODE.FEATURE ) {
                                final String runMode = atts.getValue("modes");
                                if ( runMode == null || runMode.trim().length() == 0 ) {
                                    throw new SAXException("Required attribute runModes missing for runMode element");
                                }
                                this.feature = result.getOrCreateFeature(runMode.split(","));
                                this.startLevel = 0;

                            } else {
                                this.text = new StringBuilder();
                            }
                        }
                    }
                    elementsStack.push(localName);
                }

                @Override
                public void endElement(final String uri, final String localName, final String qName)
                throws SAXException {
                    final String openElement = this.elementsStack.pop();
                    if ( !openElement.equals(localName) ) {
                        throw new SAXException("Invalid document - expected closing " + openElement + " but received " + localName);
                    }
                    if ( (uri == null && this.namespace == null) || (uri != null && uri.equals(this.namespace)) ) {
                        String textValue = (text != null ? text.toString() : null);
                        if ( textValue != null ) {
                            textValue = textValue.trim();
                            if ( textValue.length() == 0 ) {
                                textValue = null;
                            }
                        }
                        text = null;
                        boolean found = false;
                        final MODE prevMode = this.mode;
                        for(final MODE m : MODE.values()) {
                            if ( this.mode == m && localName.equals(m.elementName) ) {
                                this.mode = m.fromMode;
                                found = true;
                                break;
                            }
                        }
                        if ( !found ) {
                            throw new SAXException("Unknown element " + localName);
                        }
                        if ( prevMode == MODE.STARTLEVEL || prevMode == MODE.FEATURE_STARTLEVEL ) {
                            this.startLevel = 0;
                        } else if ( prevMode == MODE.CONFIGURATION || prevMode == MODE.FEATURE_CONFIGURATION ) {
                            ByteArrayInputStream bais = null;
                            try {
                                bais = new ByteArrayInputStream(textValue.getBytes("UTF-8"));
                                @SuppressWarnings("unchecked")
                                final Dictionary<String, Object> props = ConfigurationHandler.read(bais);
                                final Enumeration<String> e = props.keys();
                                while ( e.hasMoreElements() ) {
                                    final String key = e.nextElement();
                                    this.configuration.addProperty(key, props.get(key));
                                }
                            } catch ( final IOException ioe ) {
                                throw new SAXException(ioe);
                            } finally {
                                if ( bais != null ) {
                                    try {
                                        bais.close();
                                    } catch ( final IOException ignore ) {
                                        // ignore
                                    }
                                }
                            }
                            this.configuration = null;
                        } else if ( prevMode == MODE.SETTINGS || prevMode == MODE.FEATURE_SETTINGS) {
                            this.feature.getSettings().properties = textValue;
                        } else if ( prevMode == MODE.FEATURE ) {
                            this.feature = result.getOrCreateFeature(null);
                            this.startLevel = 0;
                        } else if ( prevMode == MODE.PROPERTIES ) {
                            final LineNumberReader reader = new LineNumberReader(new StringReader(textValue));
                            String line = null;
                            try {
                                while ( (line = reader.readLine()) != null ) {
                                    final int pos = line.indexOf("=");
                                    if ( pos == -1 || line.indexOf("=", pos + 1 ) != -1 ) {
                                        throw new SAXException("Invalid property definition: " + line);
                                    }
                                    result.addProperty(line.substring(0, pos), line.substring(pos + 1));
                                }
                            } catch (final IOException io) {
                                throw new SAXException(io);
                            }
                        }
                    }
                }

                @Override
                public void characters(final char[] ch, final int start, final int length)
                        throws SAXException {
                    if ( text != null ) {
                        text.append(ch, start, length);
                    }
                }

                @Override
                public void startDocument() throws SAXException {
                    // nothing to do
                }

                @Override
                public void skippedEntity(final String name) throws SAXException {
                    // nothing to do
                }

                @Override
                public void setDocumentLocator(final Locator locator) {
                    // nothing to do
                }

                @Override
                public void processingInstruction(final String target, final String data)
                        throws SAXException {
                    // nothing to do
                }

                @Override
                public void ignorableWhitespace(final char[] ch, final int start,final  int length)
                throws SAXException {
                    // nothing to do
                }

                @Override
                public void startPrefixMapping(final String prefix, final String uri)
                throws SAXException {
                    // nothing to do
                }

                @Override
                public void endPrefixMapping(final String prefix) throws SAXException {
                    // nothing to do
                }

                @Override
                public void endDocument() throws SAXException {
                    // nothing to do
                }

            });
            xmlReader.parse(new InputSource(reader));

            try {
                result.validate();
            } catch ( final IllegalStateException ise) {
                throw (IOException)new IOException("Invalid subsystem definition: " + ise.getMessage()).initCause(ise);
            }
            return result;
        } catch ( final SAXException se) {
            throw new IOException(se);
        } catch ( final ParserConfigurationException pce ) {
            throw new IOException(pce);
        }
    }
}
