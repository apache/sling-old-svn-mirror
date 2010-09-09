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

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

public class ModuleObject extends NativeObject implements Scriptable {

    private static final long serialVersionUID = -7538465610554258220L;

    private final ModuleScope module;

        ModuleObject(ModuleScope parent) {
            setParentScope(parent);
            setPrototype(getObjectPrototype(parent));
            this.module = parent;
        }

        @Override
        protected Object equivalentValues(Object value) {
            if (value instanceof String) {
                return this.module.getModuleName().equals(value) ? Boolean.TRUE : Boolean.FALSE;
            }
            return NOT_FOUND;
        }
}
