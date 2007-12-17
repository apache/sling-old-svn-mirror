/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.scripting.api;

import java.io.Reader;
import java.io.StringReader;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public abstract class AbstractSlingScriptEngine extends AbstractScriptEngine {

    private final ScriptEngineFactory scriptEngineFactory;
    
    protected AbstractSlingScriptEngine(ScriptEngineFactory scriptEngineFactory) {
        this.scriptEngineFactory = scriptEngineFactory;
    }
    
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    public Object eval(String script, ScriptContext context) throws ScriptException {
        StringReader reader = new StringReader(script);
        return eval(reader, context);
    }

    public ScriptEngineFactory getFactory() {
        return scriptEngineFactory;
    }

}
