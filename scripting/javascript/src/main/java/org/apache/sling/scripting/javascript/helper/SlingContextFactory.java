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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/**
 * The <code>SlingContextFactory</code> extends the standard Rhino
 * ContextFactory to provide customized settings, such as having the dynamic
 * scope feature enabled by default. Other functionality, which may be added
 * would be something like a configurable maximum script runtime value.
 */
public class SlingContextFactory extends ContextFactory {

    // conditionally setup the global ContextFactory to be ours. If
    // a global context factory has already been set, we have lost
    // and cannot set this one.
    public static void setup() {
        if (!hasExplicitGlobal()) {
            initGlobal(new SlingContextFactory());
        }
    }
    
    // private as instances of this class are only used by setup()
    private SlingContextFactory() {}
    
    @Override
    protected boolean hasFeature(Context cx, int featureIndex) {
        if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE) {
            return true;
        }

        return super.hasFeature(cx, featureIndex);
    }

}
