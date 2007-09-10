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

import javax.servlet.http.HttpServletResponseWrapper;

/**
 * The <code>ComponentResponseWrapper</code> class is a default wrapper class
 * around a {@link ComponentResponse} which may be extended to amend the
 * functionality of the original response object.
 */
public class ComponentResponseWrapper extends HttpServletResponseWrapper implements ComponentResponse {

    public ComponentResponseWrapper(ComponentResponse delegatee) {
        super(delegatee);
    }

    /**
     * Return the original {@link ComponentResponse} object wrapped by this.
     */
    public ComponentResponse getComponentResponse() {
        return (ComponentResponse) getResponse();
    }

    /**
     * @return
     * @see org.apache.sling.component.ComponentResponse#getContentType()
     */
    public String getContentType() {
        return getComponentResponse().getContentType();
    }

    /**
     * @return
     * @see org.apache.sling.component.ComponentResponse#getNamespace()
     */
    public String getNamespace() {
        return getComponentResponse().getNamespace();
    }

    public void setCharacterEncoding(String charset) {
        getComponentResponse().setCharacterEncoding(charset);
    }
}
