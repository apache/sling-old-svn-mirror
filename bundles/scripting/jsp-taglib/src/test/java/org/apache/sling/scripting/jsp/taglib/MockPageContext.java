package org.apache.sling.scripting.jsp.taglib;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

public class MockPageContext extends PageContext {

	private Map<String, Object> attributes = new HashMap<String, Object>();

	@Override
	public void forward(String arg0) throws ServletException, IOException {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public Exception getException() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public Object getPage() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public ServletRequest getRequest() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public ServletResponse getResponse() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public ServletConfig getServletConfig() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public ServletContext getServletContext() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public HttpSession getSession() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public void handlePageException(Exception arg0) throws ServletException,
			IOException {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public void handlePageException(Throwable arg0) throws ServletException,
			IOException {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public void include(String arg0) throws ServletException, IOException {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public void include(String arg0, boolean arg1) throws ServletException,
			IOException {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public void initialize(Servlet arg0, ServletRequest arg1,
			ServletResponse arg2, String arg3, boolean arg4, int arg5,
			boolean arg6) throws IOException, IllegalStateException,
			IllegalArgumentException {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public void release() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public Object findAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public Object getAttribute(String key, int arg1) {
		return attributes.get(key);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getAttributeNamesInScope(int arg0) {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public int getAttributesScope(String arg0) {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public ExpressionEvaluator getExpressionEvaluator() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public JspWriter getOut() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public VariableResolver getVariableResolver() {
		throw new java.lang.UnsupportedOperationException();
	}

	@Override
	public void removeAttribute(String arg0) {
		attributes.remove(arg0);
	}

	@Override
	public void removeAttribute(String arg0, int arg1) {
		attributes.remove(arg0);
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		attributes.put(arg0, arg1);
	}

	@Override
	public void setAttribute(String arg0, Object arg1, int arg2) {
		attributes.put(arg0, arg1);
	}

}
