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
package org.apache.sling.scripting.xproc.xpl.impl;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.sling.scripting.xproc.xpl.XplConstants;
import org.apache.sling.scripting.xproc.xpl.api.XplElement;
import org.apache.sling.scripting.xproc.xpl.api.XplElementFactory;

public class XplElementFactoryImpl implements XplElementFactory {
	
	public XplElement createXplElement(QName type, Map<String, String> attributes) {
		
		XplElement xplElement;
		
		if (XplConstants.QNAME_PIPELINE.equals(type)) {
			xplElement = new PipelineImpl();
		} else if (XplConstants.QNAME_XSLT.equals(type)) {
			xplElement = new XsltImpl();
		} else if (XplConstants.QNAME_INPUT.equals(type)) {
			xplElement = new InputImpl();
		} else if (XplConstants.QNAME_DOCUMENT.equals(type)) {
			xplElement = new DocumentImpl();
		} else {
			throw new IllegalArgumentException("An xpl element of type '" + type + "' could not be created.");
		}
		
		xplElement.setAttributes(attributes);
		
		return xplElement;
	}

}
