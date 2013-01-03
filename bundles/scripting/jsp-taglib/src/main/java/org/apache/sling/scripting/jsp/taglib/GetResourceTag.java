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

import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag for retrieving a resource based a path.
 */
public class GetResourceTag extends TagSupport {

	private static final Logger log = LoggerFactory
			.getLogger(GetResourceTag.class);
	private static final long serialVersionUID = -1945089681840552408L;
	private String path;
	private String var;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
	 */
	@Override
	public int doEndTag() {
		log.trace("doEndTag");

		log.debug("Retrieving resource at path: " + path);

		ResourceResolver resolver = getResourceResolver();
		final Resource resource = resolver.getResource(path);

		log.debug("Saving " + resource + " to variable " + var);
		pageContext.setAttribute(var, resource);

		return EVAL_PAGE;
	}

	/**
	 * Method for retrieving the ResourceResolver from the page context.
	 * 
	 * @return the resource resolver
	 */
	protected ResourceResolver getResourceResolver() {
		final SlingBindings bindings = (SlingBindings) pageContext.getRequest()
				.getAttribute(SlingBindings.class.getName());
		final SlingScriptHelper scriptHelper = bindings.getSling();
		final ResourceResolver resolver = scriptHelper.getRequest()
				.getResourceResolver();
		return resolver;
	}

	/**
	 * Get the path of the resource to retrieve.
	 * 
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Gets the variable name to which to save the list of children.
	 * 
	 * @return the variable name
	 */
	public String getVar() {
		return var;
	}

	/**
	 * Set the path of the resource to retrieve.
	 * 
	 * @param path
	 *            the path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Sets the variable name to which to save the list of children.
	 * 
	 * @param var
	 *            the variable name
	 */
	public void setVar(String var) {
		this.var = var;
	}
}
