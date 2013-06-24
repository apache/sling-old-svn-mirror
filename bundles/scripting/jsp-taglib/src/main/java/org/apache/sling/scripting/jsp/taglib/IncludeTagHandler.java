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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;

import org.apache.commons.io.IOUtils;

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
        	String encoding = response.getCharacterEncoding();
        	BufferedServletOutputStream bsops = new BufferedServletOutputStream(encoding);
        	try{
	        	CaptureResponseWrapper wrapper = new CaptureResponseWrapper((HttpServletResponse) response, bsops);
	        	dispatcher.include(request, wrapper);
	        	if (! wrapper.isBinaryResponse()) {
	        		wrapper.flushBuffer();
	            	pageContext.setAttribute(var, bsops.getBuffer(), scope);
	        	}
        	}finally{
        		IOUtils.closeQuietly(bsops);
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
    
    /**
     * Extends the HttpServletResponse to wrap the response and capture the results.
     */
    private static final class CaptureResponseWrapper extends HttpServletResponseWrapper {
		private final String encoding;
		private final ServletOutputStream ops;
		private boolean isBinaryResponse = false;
		private PrintWriter writer = null;

		/**
		 * Construct a new CaptureResponseWrapper.
		 * 
		 * @param response
		 *            the response to wrap
		 * @param ops
		 *            the output stream to write to
		 */
		CaptureResponseWrapper(HttpServletResponse response,
				ServletOutputStream ops) {
			super(response);
			this.encoding = response.getCharacterEncoding();
			this.ops = ops;
		}

		/**
		 * Returns true if the response is binary.
		 * 
		 * @return
		 */
    	public boolean isBinaryResponse() {
    		return isBinaryResponse;
    	}
    	
    	
    	/*
    	 * (non-Javadoc)
    	 * @see javax.servlet.ServletResponseWrapper#flushBuffer()
    	 */
    	@Override
		public void flushBuffer() throws IOException {
    		if (isBinaryResponse()) {
    			getResponse().getOutputStream().flush();
    		} else {
    			writer.flush();
    		}
		}

    	/*
    	 * (non-Javadoc)
    	 * @see javax.servlet.ServletResponseWrapper#getOutputStream()
    	 */
		@Override
    	public ServletOutputStream getOutputStream() throws IOException {
    		if (writer != null) {
    			throw new IOException("'getWriter()' has already been invoked for a character data response.");
    		}
    		isBinaryResponse = true;
    		return getResponse().getOutputStream();
    	}
    	
		/*
		 * (non-Javadoc)
		 * @see javax.servlet.ServletResponseWrapper#getWriter()
		 */
    	@Override
    	public PrintWriter getWriter() throws IOException {
    		if (writer != null) {
    			return writer;
    		}
    		if (isBinaryResponse) {
    			throw new IOException("'getOutputStream()' has already been invoked for a binary data response.");
    		}
    		writer = new PrintWriter(new OutputStreamWriter(ops, encoding));
    		return writer;
    	}
    	
    }
    
    /**
     * Extends the ServletOutputStream to capture the results into a byte array.
     */
    private static final class BufferedServletOutputStream extends ServletOutputStream {
    	private final ByteArrayOutputStream baops = new ByteArrayOutputStream();
    	private final String encoding;
    	
    	/**
    	 * Constructs a new BufferedServletOutputStream.
    	 * 
    	 * @param encoding the encoding string
    	 */
    	public BufferedServletOutputStream(String encoding) {
			this.encoding = encoding;
		}

		/**
    	 * Gets the byte buffer as a string.
    	 * 
    	 * @return the byte buffer
		 * @throws IOException
    	 */
    	public String getBuffer() throws IOException {
    		return baops.toString(encoding);
    	}
    	
    	/*
    	 * (non-Javadoc)
    	 * @see java.io.OutputStream#close()
    	 */
    	@Override
    	public void close() throws IOException {
    		baops.reset();
    		super.close();
    	}
    	
    	/*
    	 * (non-Javadoc)
    	 * @see java.io.OutputStream#write(int)
    	 */
    	@Override
    	public void write(int b) throws IOException {
    		baops.write(b);
    	}
    }
}
