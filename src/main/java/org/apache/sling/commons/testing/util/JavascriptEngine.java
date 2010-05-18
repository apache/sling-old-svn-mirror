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
package org.apache.sling.commons.testing.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Simplistic Javascript engine using Rhino, meant 
 * 	for automated tests */
public class JavascriptEngine {
	/** Execute supplied code against supplied data, 
	 * 	see JavascriptEngineTest for examples */
	public String execute(String code, String jsonData) throws IOException {
        final String jsCode = "data=" + jsonData + ";\n" + code;
        final Context rhinoContext = Context.enter();
        final ScriptableObject scope = rhinoContext.initStandardObjects();

        // execute the script, out script variable maps to sw
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        ScriptableObject.putProperty(scope, "out", Context.javaToJS(pw, scope));
        final int lineNumber = 1;
        final Object securityDomain = null;
        try {
            rhinoContext.evaluateString(
            		scope, 
            		jsCode, 
            		getClass().getSimpleName(),
                    lineNumber, 
                    securityDomain);
        } catch(Exception e) {
        	final IOException ioe = new IOException("While executing [" + code + "]:" + e);
        	ioe.initCause(e);
        	throw ioe;
        }

        // check script output
        pw.flush();
        return sw.toString().trim();
    }
}
