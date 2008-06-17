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
package org.apache.sling.scripting.jst;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.jcr.RepositoryException;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JST script engine.
 *  This engine does not really execute the supplied script: it dumps a default
 *  HTML representation of the current Resource, which includes a reference
 *  to the script path with a .jst.js selector and extension, so that the
 *  {@link JsCodeGeneratorServlet} can provide the template-specific client
 *  javascript code.
 */
public class JstScriptEngine extends AbstractSlingScriptEngine {

    private final HtmlCodeGenerator htmlGenerator;
    private final Logger log = LoggerFactory.getLogger(getClass());

    JstScriptEngine(ScriptEngineFactory scriptEngineFactory) {
        super(scriptEngineFactory);
        htmlGenerator = new HtmlCodeGenerator();
    }

    /** Output the HTML representation, with reference to the actual client-side script */ 
    public Object eval(Reader script, ScriptContext context) throws ScriptException {

        final Bindings props = context.getBindings(ScriptContext.ENGINE_SCOPE);
        final SlingScriptHelper helper = (SlingScriptHelper) props.get(SlingBindings.SLING);
        final InputStream scriptStream = helper.getScript().getScriptResource().adaptTo(InputStream.class);
        
        try {
            htmlGenerator.generateHtml(helper.getRequest(), 
                    helper.getScript().getScriptResource().getPath(), scriptStream,
                    helper.getResponse().getWriter());
                    
        } catch (IOException ioe) {
            throw new ScriptException(ioe);
            
        } catch(RepositoryException re) {
            throw new ScriptException(re);
            
        } catch(JSONException je) {
            throw new ScriptException(je);
            
        } finally {
            if(scriptStream != null) {
                try { 
                    scriptStream.close();
                } catch(IOException ioe) {
                    log.warn("IOException while closing scriptStream",ioe);
                }
            }
        }

        return null;
    }
    
}