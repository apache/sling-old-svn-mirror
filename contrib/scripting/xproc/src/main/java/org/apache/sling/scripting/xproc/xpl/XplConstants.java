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

import javax.xml.namespace.QName;

public class XplConstants {
	
	public static final String NS_XPROC = "http://www.w3.org/ns/xproc";
	
	public static final QName QNAME_PIPELINE = new QName(NS_XPROC, "pipeline"); 
	public static final QName QNAME_XSLT = new QName(NS_XPROC, "xslt");
	public static final QName QNAME_INPUT = new QName(NS_XPROC, "input");
	public static final QName QNAME_DOCUMENT = new QName(NS_XPROC, "document");
	
}
