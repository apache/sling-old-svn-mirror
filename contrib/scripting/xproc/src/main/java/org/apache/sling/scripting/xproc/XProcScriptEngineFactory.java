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
package org.apache.sling.scripting.xproc;

import javax.script.ScriptEngine;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;

public class XProcScriptEngineFactory extends AbstractScriptEngineFactory {
	
    public final static String XPROC_SCRIPT_EXTENSION = "xpl";

    public final static String XPROC_MIME_TYPE = "application/xml";

    public final static String SHORT_NAME = "XProc";

    private static final String XPROC_NAME = "XMLProc";

    private static final String DEFAULT_XPROC_VERSION = "1.0";
	
	public XProcScriptEngineFactory() {
		setExtensions(XPROC_SCRIPT_EXTENSION);
		setMimeTypes(XPROC_MIME_TYPE);
		setNames(SHORT_NAME, "xml processing", "xml pipeline processor");
	}
	
	public ScriptEngine getScriptEngine() {
		return new XProcScriptEngine(this);
	}
	
	public String getLanguageName() {
		return XPROC_NAME;
	}

	public String getLanguageVersion() {
		return DEFAULT_XPROC_VERSION;
	}

}
