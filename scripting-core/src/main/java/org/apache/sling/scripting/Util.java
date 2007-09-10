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
package org.apache.sling.scripting;

import org.apache.sling.component.ComponentRequest;

public class Util {

    public static final String ATTR_COMPONENT = "org.apache.sling.scripting.component";
    public static final String ATTR_RENDER_REQUEST = "org.apache.sling.scripting.render_request";
    public static final String ATTR_RENDER_RESPONSE = "org.apache.sling.scripting.render_response";

    public static Object replaceAttribute(ComponentRequest request, String attrName, Object value) {
        // get the old value
        Object old = request.getAttribute(attrName);
        
        // set new value or remove the value
        if (value != null) {
            request.setAttribute(attrName, value);
        } else {
            request.removeAttribute(attrName);
        }
        
        // return the old value
        return old;
    }

}
