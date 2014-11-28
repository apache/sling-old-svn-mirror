/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.js.impl.cjs;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * CommonJS module implementation
 */
public class CommonJsModule extends ScriptableObject {

    private static final String EXPORTS = "exports";

    private Object exports = new ExportsObject();
    private boolean modifiedModule;


    @Override
    public Object get(String name, Scriptable start) {
        if (name.equals(EXPORTS)) {
            return exports;
        }
        return super.get(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (name.equals(EXPORTS)) {
            setExports(value);
        } else {
            super.put(name, start, value);
        }
    }

    public Object getExports() {
        return exports;
    }

    public void setExports(Object exports) {
        modifiedModule = true;
        this.exports = exports;
    }

    public boolean isModified() {
        return modifiedModule || ((ExportsObject) exports).isModified();
    }

    @Override
    public String getClassName() {
        return "Module";
    }
}
