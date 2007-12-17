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

import javax.jcr.Node;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.javascript.wrapper.ScriptableNode;
import org.apache.sling.scripting.javascript.wrapper.ScriptableResource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlingWrapFactory extends WrapFactory {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final WrapFactory INSTANCE = new SlingWrapFactory();

    /**
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param javaObject the object to be wrapped
     * @param staticType type hint. If security restrictions prevent to wrap
     *            object based on its class, staticType will be used instead.
     * @return the wrapped value which shall not be null
     */
    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope,
            Object javaObject, Class staticType) {

        try {
            if (javaObject instanceof Resource) {
                return cx.newObject(scope, ScriptableResource.CLASSNAME,
                    new Object[] { javaObject });
            } else if (javaObject instanceof Node) {
                return cx.newObject(scope, ScriptableNode.CLASSNAME,
                    new Object[] { javaObject });
            }
        } catch (Exception e) {
            log.warn("Cannot Wrap " + javaObject, e);
        }

        return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
    }

}
