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
package org.apache.sling.scripting;

import org.apache.sling.component.Component;


/**
 * The <code>ScriptHandler</code> service interface may be implemented by
 * bundles implementing support for scripting languages such as JSP, ECMA
 * or JSR-223 scripting.
 */
public interface ScriptHandler {

    /**
     * Returns the type of script supported by this handler. This value is
     * compared to the script type of the ScriptedComponent.
     * <p>
     * For example a handler for JSP scripts might return <em>jsp</em>.
     */
    String getType();

    /**
     * Returns a {@link ComponentRenderer} called by the ScriptedComponent
     * to actually executed the script on behalf of the component. If the
     * handler cannot find the name script, <code>null</code> is returned.
     */
    ComponentRenderer getComponentRenderer(Component component, String scriptName);
}
