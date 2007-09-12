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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;

/**
 * The <code>HttpServletAdapter</code> TODO
 */
public abstract class HttpServletAdapter implements ComponentRenderer {

    /**
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    protected abstract void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException;


    public void service(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {

        Object oldRequest = Util.replaceAttribute(request, Util.ATTR_RENDER_REQUEST, request);
        Object oldResponse = Util.replaceAttribute(request, Util.ATTR_RENDER_RESPONSE, response);

        try {
            this.service(request, response);
        } catch (IOException ioe) {
            // forward
            throw ioe;
        } catch (ServletException se) {
            // wrap as ComponentException
            throw new ComponentException(se.getMessage(), se);
        } catch (RuntimeException re) {
            // forward
            throw re;
        } catch (Throwable t) {
            // unexpected
            throw new ComponentException(t.getMessage(), t);
        } finally {
            Util.replaceAttribute(request, Util.ATTR_RENDER_REQUEST, oldRequest);
            Util.replaceAttribute(request, Util.ATTR_RENDER_RESPONSE, oldResponse);
        }
    }
}
