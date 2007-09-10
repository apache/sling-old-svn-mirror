/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.component;

/**
 * The <code>ComponentSessionUtil</code> class helps identify and decode
 * attributes in the <code>COMPONENT_SCOPE</code> scope of the
 * ComponentSession when accessed through the HttpSession and from within calls
 * to methods of the HttpSessionBindingListener interface.
 */
public class ComponentSessionUtil {

    /**
     * Returns the attribute name of an attribute in the
     * <code>COMPONENT_SCOPE</code>. If the attribute is in the
     * <code>APPLICATION_SCOPE</code> it returns the attribute name unchanged.
     * 
     * @param name a string specifying the name of the encoded component session
     *            attribute
     * @return the decoded attribute name
     */
    public static String decodeAttributeName(String name) {
        if (name.startsWith(ComponentSession.COMPONENT_SCOPE_NAMESPACE)) {
            int index = name.indexOf('?');
            if (index > -1) {
                name = name.substring(index + 1);
            }
        }
        return name;
    }

    /**
     * Returns the component attribute scope from an encoded component
     * attribute.
     * <p>
     * Possible return values are:
     * <ul>
     * <li><code>ComponentSession.APPLICATION_SCOPE</code></li>
     * <li><code>ComponentSession.COMPONENT_SCOPE</code></li>
     * </ul>
     * 
     * @param name a string specifying the name of the encoded component
     *            attribute
     * @return the decoded attribute scope
     * @see ComponentSession
     */
    public static int decodeScope(String name) {
        int scope = ComponentSession.APPLICATION_SCOPE;
        if (name.startsWith(ComponentSession.COMPONENT_SCOPE_NAMESPACE)) {
            int index = name.indexOf('?');
            if (index > -1) {
                scope = ComponentSession.COMPONENT_SCOPE;
            }
        }
        return scope;
    }
}
