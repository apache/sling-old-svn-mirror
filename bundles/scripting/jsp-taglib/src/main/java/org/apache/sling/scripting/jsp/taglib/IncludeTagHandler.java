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
package org.apache.sling.scripting.jsp.taglib;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;

import org.apache.sling.scripting.core.servlet.CaptureResponseWrapper;

/**
 * The <code>IncludeTagHandler</code> implements the
 * <code>&lt;sling:include&gt;</code> custom tag.
 */
public class IncludeTagHandler extends AbstractDispatcherTagHandler {

    private static final long serialVersionUID = 2835586777145471683L;

    /** flush argument */
    private boolean flush = false;
    private String var = null;
    private Integer scope = PageContext.PAGE_SCOPE;

    protected void dispatch(RequestDispatcher dispatcher,
            ServletRequest request, ServletResponse response)
            throws IOException, ServletException {

        // optionally flush
        if (flush && !(pageContext.getOut() instanceof BodyContent)) {
            // might throw an IOException of course
            pageContext.getOut().flush();
        }
        if (var == null) {
        	dispatcher.include(request, response);
        } else {
			final CaptureResponseWrapper wrapper = new CaptureResponseWrapper((HttpServletResponse) response);
			dispatcher.include(request, wrapper);
			if (!wrapper.isBinaryResponse()) {
				pageContext.setAttribute(var, wrapper.getCapturedCharacterResponse(), scope);
			}
		}
    }

    public void setPageContext(PageContext pageContext) {
        super.setPageContext(pageContext);

        // init local fields, since tag might be reused
        flush = false;
        this.var = null;
        this.scope = PageContext.PAGE_SCOPE;
    }

    public void setFlush(boolean flush) {
        this.flush = flush;
    }
    
    public void setVar(String var) {
    	if (var != null && var.trim().length() > 0) {
    		this.var = var;
    	}
    }
    
    // for tag attribute
    public void setScope(String scope) {
    	this.scope = validScope(scope);
    }
    
	/**
	 * Gets the int code for a valid scope, must be one of 'page', 'request',
	 * 'session' or 'application'. If an invalid or no scope is specified page
	 * scope is returned.
	 * 
	 * @param scope
	 * @return
	 */
	private Integer validScope(String scope) {
		scope = (scope != null && scope.trim().length() > 0 ? scope.trim()
				.toUpperCase() : null);
		if (scope == null) {
			return PageContext.PAGE_SCOPE;
		}

		String[] scopes = { "PAGE", "REQUEST", "SESSION", "APPLICATION" };
		Integer[] iaScopes = { PageContext.PAGE_SCOPE,
				PageContext.REQUEST_SCOPE, PageContext.SESSION_SCOPE,
				PageContext.APPLICATION_SCOPE };

		for (int ndx = 0, len = scopes.length; ndx < len; ndx++) {
			if (scopes[ndx].equals(scope)) {
				return iaScopes[ndx];
			}
		}
		return PageContext.PAGE_SCOPE;
    }

}
