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

import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.xproc.xpl.XplBuilder;
import org.apache.sling.scripting.xproc.xpl.api.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ScriptEngine} that uses XPL definition files
 * in order to execute pipelines over Sling resources.
 * 
 * @see http://www.w3.org/TR/xproc/
 */
public class XProcScriptEngine extends AbstractSlingScriptEngine {
	
	private static final Logger log = LoggerFactory.getLogger(XProcScriptEngine.class);
	
	protected XProcScriptEngine(ScriptEngineFactory factory) {
		super(factory);
	}

	public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
		Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
		SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
		if (helper == null) {
			throw new ScriptException("SlingScriptHelper missing from bindings");
		}
		
		String scriptName = helper.getScript().getScriptResource().getPath();
		
		try {
			XplBuilder xplBuilder = new XplBuilder();
			Pipeline xpl = (Pipeline) xplBuilder.build(reader);
			xpl.getEnv().setSling(helper);
			xpl.eval();
		} catch (Throwable t) {
			log.error("Failure running XProc script.", t);
      final ScriptException se = new ScriptException("Failure running XProc script " + scriptName);
      se.initCause(t);
      throw se;
		}
		
		return null;
	}

}