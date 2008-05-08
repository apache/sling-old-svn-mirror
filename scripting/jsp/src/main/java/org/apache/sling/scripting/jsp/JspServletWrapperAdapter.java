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
package org.apache.sling.scripting.jsp;

import java.io.IOException;

import javax.script.Bindings;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.jsp.jasper.Constants;
import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext;
import org.apache.sling.scripting.jsp.jasper.servlet.JspServletWrapper;

/**
 * The <code>JspServletWrapperAdapter</code> TODO
 */
public class JspServletWrapperAdapter extends JspServletWrapper {

    JspServletWrapperAdapter(ServletConfig config, Options options,
            String jspUri, boolean isErrorPage, JspRuntimeContext rctxt)
            throws JasperException {
        super(config, options, jspUri, isErrorPage, rctxt);
    }

    /**
     * @param bindings
     * @throws SlingIOException
     * @throws SlingServletException
     * @throws IllegalArgumentException if the Jasper Precompile controller
     *             request parameter has an illegal value.
     */
    public void service(Bindings bindings) {
        final SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        final Object oldValue = request.getAttribute(Bindings.class.getName());
        try {
            request.setAttribute(Bindings.class.getName(), bindings);
            service(request, (SlingHttpServletResponse)bindings.get(SlingBindings.RESPONSE), preCompile(request));
        } catch (SlingException se) {
            // rethrow as is
            throw se;
        } catch (IOException ioe) {
            throw new SlingIOException(ioe);
        } catch (ServletException se) {
            throw new SlingServletException(se);
        } finally {
            request.setAttribute(Bindings.class.getName(), oldValue);
        }
    }

    /**
     * <p>
     * Look for a <em>precompilation request</em> as described in Section
     * 8.4.2 of the JSP 1.2 Specification. <strong>WARNING</strong> - we cannot
     * use <code>request.getParameter()</code> for this, because that will
     * trigger parsing all of the request parameters, and not give a servlet the
     * opportunity to call <code>request.setCharacterEncoding()</code> first.
     * </p>
     *
     * @param request The servlet requset we are processing
     * @throws IllegalArgumentException if an invalid parameter value for the
     *             <code>jsp_precompile</code> parameter name is specified
     */
    boolean preCompile(HttpServletRequest request) {

        // assume it is ok to access the parameters here, as we are not a
        // toplevel servlet
        String jspPrecompile = request.getParameter(Constants.PRECOMPILE);
        if (jspPrecompile == null) {
            return false;
        }

        if (jspPrecompile.length() == 0) {
            return true; // ?jsp_precompile
        }

        if (jspPrecompile.equals("true")) {
            return true; // ?jsp_precompile=true
        }

        if (jspPrecompile.equals("false")) {
            // Spec says if jsp_precompile=false, the request should not
            // be delivered to the JSP page; the easiest way to implement
            // this is to set the flag to true, and precompile the page
            // anyway.
            // This still conforms to the spec, since it says the
            // precompilation request can be ignored.
            return true; // ?jsp_precompile=false
        }

        // unexpected value, fail
        throw new IllegalArgumentException("Cannot have request parameter "
            + Constants.PRECOMPILE + " set to " + jspPrecompile);
    }
}
