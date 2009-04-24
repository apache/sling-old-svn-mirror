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
package org.apache.sling.scripting.xproc.xpl;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.sling.scripting.xproc.xpl.api.Step;
import org.apache.sling.scripting.xproc.xpl.api.XplElement;
import org.apache.sling.scripting.xproc.xpl.api.XplElementFactory;
import org.apache.sling.scripting.xproc.xpl.impl.XplElementFactoryImpl;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Builds a W3C XML Processing pipeline
 * from a XPL file.
 */
public class XplBuilder {
	
	private XplElementFactory xplElementFactory;
	
	public XplBuilder() {
		this.xplElementFactory = new XplElementFactoryImpl();
	}
	
	public Step build(Reader xplReader) throws Exception {
		XMLReader xMLReader =  XMLReaderFactory.createXMLReader();
		XplHandler xplHandler = new XplHandler();
		xMLReader.setContentHandler(xplHandler);
		InputSource inputSource = new InputSource(xplReader);
		xMLReader.parse(inputSource);
		return xplHandler.getRootStep();
	}
	
	protected XplElement createXplElement(String localName, Map<String, String> attributes) {
        return XplBuilder.this.xplElementFactory.createXplElement(new QName(XplConstants.NS_XPROC, localName), attributes);
    }
	
	class XplHandler extends DefaultHandler {
		
		private XplElement currentXplElement;
		private Step rootStep;
		
		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			if (this.currentXplElement == null) {
                throw new IllegalStateException("Received closing '" + localName + "' but there was no element to close.");
            }
			
			this.currentXplElement = this.currentXplElement.getParent();
		}
		
		@Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }
		
		public Step getRootStep() {
			return this.rootStep;
		}
		
		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (this.currentXplElement == null) {
				if (XplConstants.QNAME_PIPELINE.getLocalPart().equals(localName)) {
					this.rootStep = (Step) XplBuilder.this.createXplElement(localName, null);
					this.currentXplElement = this.rootStep;
					return;
				}
				throw new IllegalStateException("Expected 'pipeline' as first element, but received '" + localName + "'");
			}
			
			Map<String, String> atts = new HashMap<String, String>();
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
            	atts.put(attributes.getQName(i), attributes.getValue(i));
            }
			
			XplElement xplElement = createXplElement(localName, atts);
			this.currentXplElement.addChild(xplElement);
			this.currentXplElement = xplElement;
		}
		
	}
	
}
