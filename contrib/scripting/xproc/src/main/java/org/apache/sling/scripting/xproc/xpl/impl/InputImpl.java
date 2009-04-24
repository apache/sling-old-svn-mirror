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

import javax.xml.namespace.QName;

import org.apache.sling.scripting.xproc.xpl.XplConstants;
import org.apache.sling.scripting.xproc.xpl.api.Document;
import org.apache.sling.scripting.xproc.xpl.api.Input;

public class InputImpl extends AbstractXplElementImpl implements Input {

	private String port;
	private Document document;
	
	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}
	
	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}
	
	@Override
	public QName getQName() {
		return XplConstants.QNAME_INPUT;
	}
	
}
