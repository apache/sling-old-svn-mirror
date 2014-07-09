/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.python;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.python.util.PythonInterpreter;

/**
 * Python ScriptEngine
 */
public class PythonScriptEngine extends AbstractSlingScriptEngine {

	private PythonInterpreter interp;

	public PythonScriptEngine(PythonScriptEngineFactory factory) {
		super(factory);

		final ClassLoader oldClassLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					getClass().getClassLoader());

			interp = new PythonInterpreter();
			// interp.initialize(System.getProperties(), null, null);

		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
	}

	public Object eval(Reader script, ScriptContext scriptContext)
			throws ScriptException {
		Bindings bindings = scriptContext
				.getBindings(ScriptContext.ENGINE_SCOPE);

		SlingScriptHelper helper = (SlingScriptHelper) bindings
				.get(SlingBindings.SLING);
		if (helper == null) {
			throw new ScriptException("SlingScriptHelper missing from bindings");
		}

		// ensure GET request
		if (helper.getRequest() != null && !"GET".equals(helper.getRequest().getMethod())) {
			throw new ScriptException(
					"Python scripting only supports GET requests");
		}

		final ClassLoader oldClassLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					getClass().getClassLoader());
			StringBuffer scriptString = new StringBuffer();
			BufferedReader bufferedScript = new BufferedReader(script);
			String nextLine = bufferedScript.readLine();
			String newLine = System.getProperty("line.separator");
			while (nextLine != null) {
				scriptString.append(nextLine);
				scriptString.append(newLine); 
				nextLine = bufferedScript.readLine();
			}

			// set writer
			interp.setOut(scriptContext.getWriter());
			interp.setErr(scriptContext.getErrorWriter());
			
			// set all bindings
			for (Object entryObj : bindings.entrySet()) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
				interp.set((String) entry.getKey(), entry.getValue());
			}

			// execute Python code
			interp.exec(scriptString.toString());

		} catch (Throwable t) {
			final ScriptException ex = new ScriptException(
					"Failure running Python script:" + t);
			ex.initCause(t);
			throw ex;
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
		return null;
	}
}
