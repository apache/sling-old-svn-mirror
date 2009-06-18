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
package org.apache.sling.scripting.javascript.helper;

import java.io.File;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.debugger.ScopeProvider;

/**
 * The <code>SlingContextFactory</code> extends the standard Rhino
 * ContextFactory to provide customized settings, such as having the dynamic
 * scope feature enabled by default. Other functionality, which may be added
 * would be something like a configurable maximum script runtime value.
 */
public class SlingContextFactory extends ContextFactory {

    private SlingRhinoDebugger debugger;
    private ScopeProvider scopeProvider;
    private boolean debuggerActive;
    
    // conditionally setup the global ContextFactory to be ours. If
    // a global context factory has already been set, we have lost
    // and cannot set this one.
    public static void setup(ScopeProvider sp) {
        // TODO what do we do in the other case? debugger won't work
        if (!hasExplicitGlobal()) {
            initGlobal(new SlingContextFactory(sp));
        }
    }
    
    // private as instances of this class are only used by setup()
    private SlingContextFactory(ScopeProvider sp) 
    {
        scopeProvider = sp;
        
        // TODO make this configurable via OSGi
        File f = new File("/tmp/sling.debug");
        debuggerActive = f.exists();
    }
    
    @Override
    protected Context makeContext() {
        return new SlingContext();
    }
    
    @Override
    protected boolean hasFeature(Context cx, int featureIndex) {
        if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE) {
            return true;
        }

        return super.hasFeature(cx, featureIndex);
    }
    
    @Override
    protected void onContextCreated(Context cx) {
        super.onContextCreated(cx);
        initDebugger(cx);
    }

    private void initDebugger(Context cx) {
        if(!debuggerActive) {
            return;
        }
        try {
            if (debugger == null) {
                debugger = new SlingRhinoDebugger(getClass().getSimpleName());
                debugger.setScopeProvider(scopeProvider);
                debugger.attachTo(this);
            }
        } catch (Exception e) {
            // TODO log
            System.err.println("SlingContextFactory.initDebugger(): " + e);
        }
    }
    
}
